#!/bin/sh
set -eu
for contract in qa/contracts/verify_approval_permissions.py; do
  PYTHONDONTWRITEBYTECODE=1 python3 -B "$contract"
done
PYTHONDONTWRITEBYTECODE=1 python3 -B - <<'PY'
from pathlib import Path
import json
import xml.etree.ElementTree as ET

pom = Path('repository/elm_bk/pom.xml')
package = Path('repository/elmclient/package.json')
if pom.is_file():
    ET.parse(pom)
    print('PASS module structure: Maven pom.xml is well-formed')
if package.is_file():
    data = json.loads(package.read_text(encoding='utf-8'))
    assert isinstance(data.get('scripts'), dict) and data['scripts']
    print('PASS module structure: frontend package scripts are valid JSON')
PY
if [ -d repository/scripts ]; then
  for script in repository/scripts/*.sh; do
    [ -f "$script" ] || continue
    bash -n "$script"
  done
  printf '%s
' 'PASS module structure: shell scripts passed syntax checks'
fi
