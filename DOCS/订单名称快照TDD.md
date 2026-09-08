# 订单名称快照

问题：订单查询关联实时菜名，商家改名会改变历史订单的显示。

规则：下单时由服务器把菜名、单价写进订单明细；以后查订单读这份记录，查菜单仍读当前商品。升级前没有存过菜名的订单显示“历史商品（名称未留存）”，不能把今天的菜名当成过去的菜名。

## 测试与三段提交

`OrderNameSnapshotJourneyTest` 使用真实 Spring 接口、业务层、Mapper 和数据库，只模拟登录身份。38 项包括配送/自取、未支付/完成订单、顾客/商家/旧查询入口、改名改价、多商品、反复改名、下架、逻辑删除、旧记录、权限、中文/引号/100字边界、伪造参数、库存失败回滚、幂等重试和菜单实时性。

1. RED：先提交测试和独立 MySQL 测试脚本，记录真实断言失败。
2. GREEN：最小实现使相同测试通过，跑全部后端测试和 MySQL 测试。
3. REFACTOR：去掉订单查询中不再需要的关联，保持测试不变并再次回归。

本机 Java 17 / Maven：在 `elm_bk` 运行 `mvn -Pcoverage verify`。

独立 MySQL 8.0：仓库根目录运行 `bash scripts/test-order-name-snapshot.sh check`。脚本创建并销毁自己的容器与数据库，不接触演示库；默认使用 `/Users/chengwen/.m2`，可用 `MAVEN_CACHE` 指向自己的 Maven 缓存。

脚本还测试迁移可重复执行、老记录名称保持 NULL、价格数量不变、已存快照不被覆盖。覆盖率报告在 `elm_bk/target/site/jacoco/index.html`，执行日志按阶段保存在 `elm_bk/target/order-name-snapshot`，提交用的精简输出在 `RECORDS/lcw-order-name-snapshot.txt`。

## 数据库升级

新建数据库使用 `elm_bk/elm_v2.sql`。已有数据库先备份，再单独执行 `elm_bk/db/migrations/003_order_name_snapshot.sql`，然后发布后端。不可对已有数据运行会重建表的初始化脚本。迁移只加一个允许 NULL 的字段，不回填、不删除数据。

AI 协助编写测试、最小实现和回归；阶段记录保留实际运行结果，不使用预先编造的通过日志。答辩可按“下单→商家改名→菜单变了而旧订单不变→再次下单采用新名称”演示。
