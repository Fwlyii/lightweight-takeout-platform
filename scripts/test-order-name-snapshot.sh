#!/usr/bin/env bash
# Runs only against a disposable MySQL database; never connects to the demo database.
set -euo pipefail
repo=$(cd "$(dirname "$0")/.." && pwd)
stage=${1:-check}
[[ "$stage" =~ ^[a-z-]+$ ]] || exit 2
name="order-snapshot-test-$$"
out="$repo/elm_bk/target/order-name-snapshot/$stage"
mkdir -p "$out"
cleanup() {
  docker rm -f "$name" >/dev/null 2>&1 || true
  docker network rm "$name" >/dev/null 2>&1 || true
}
trap cleanup EXIT
docker network create "$name" >/dev/null
docker run -d --rm --name "$name" --network "$name" \
  -e MYSQL_ROOT_PASSWORD=snapshot-test -e MYSQL_DATABASE=order_snapshot_test \
  mysql:8.0.40 >/dev/null
ready=false
for attempt in {1..60}; do
  if docker exec "$name" mysql -uroot -psnapshot-test -e 'SELECT 1' >/dev/null 2>&1; then
    ready=true; break
  fi
  sleep 1
done
[[ "$ready" == true ]] || { docker logs "$name"; exit 1; }
sql() { docker exec -i "$name" mysql -uroot -psnapshot-test "$@"; }
sql order_snapshot_test < "$repo/elm_bk/elm_v2.sql"
migration="$repo/elm_bk/db/migrations/003_order_name_snapshot.sql"
migration_status=0
if [[ -f "$migration" ]]; then
  sql order_snapshot_test < "$migration"
  sql -e 'CREATE DATABASE order_snapshot_migration_test;'
  sql order_snapshot_migration_test <<'SQL'
CREATE TABLE orderdetailet(id BIGINT PRIMARY KEY, food_price DECIMAL(10,2), quantity INT);
INSERT INTO orderdetailet VALUES (1,20.00,2),(2,3.00,1);
SQL
  sql order_snapshot_migration_test < "$migration"
  result=$(sql -N order_snapshot_migration_test -e "SELECT COUNT(*) FROM orderdetailet WHERE food_name_snapshot IS NULL AND ((id=1 AND food_price=20 AND quantity=2) OR (id=2 AND food_price=3 AND quantity=1));")
  [[ "$result" == 2 ]]
  sql order_snapshot_migration_test -e "UPDATE orderdetailet SET food_name_snapshot='saved name' WHERE id=1;"
  sql order_snapshot_migration_test < "$migration"
  result=$(sql -N order_snapshot_migration_test -e "SELECT COUNT(*) FROM orderdetailet WHERE id=1 AND food_name_snapshot='saved name';")
  [[ "$result" == 1 ]]
  echo 'Migration: PASS (nullable, no invented historical names, repeat-safe)'
else
  echo 'Migration: FAIL (003_order_name_snapshot.sql missing)'
  migration_status=1
fi
set +e
docker run --rm --network "$name" \
  -v "$repo/elm_bk:/workspace" -v "${MAVEN_CACHE:-/Users/chengwen/.m2}:/root/.m2" -w /workspace \
  maven:3.9.9-eclipse-temurin-17 mvn -Pcoverage \
  -Dtest=OrderNameSnapshotJourneyTest \
  "-Dsnapshot.test.jdbc-url=jdbc:mysql://$name:3306/order_snapshot_test?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=UTF-8" \
  -Dsnapshot.test.jdbc-driver=com.mysql.cj.jdbc.Driver \
  -Dsnapshot.test.jdbc-user=root -Dsnapshot.test.jdbc-password=snapshot-test \
  -Dsnapshot.test.init-mode=never verify 2>&1 | tee "$out/mysql.log"
test_status=${PIPESTATUS[0]}
set -e
cp -R "$repo/elm_bk/target/surefire-reports" "$out/mysql-reports"
exit $((test_status || migration_status))
