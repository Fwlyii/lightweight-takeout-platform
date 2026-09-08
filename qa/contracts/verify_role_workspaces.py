from pathlib import Path
import sys

root = Path(__file__).resolve().parents[2]
checks = {'elmclient/src/components/Footer.vue': ['首页', '订单', '我的'], 'elmclient/src/components/AdminFooter.vue': ['router-link', '首页'], 'elmclient/src/components/BusinessFooter.vue': ['router-link', '订单'], 'elmclient/src/components/RiderFooter.vue': ['router-link', '接单']}
missing = [path for path in checks if not (root / path).is_file()]
if missing:
    print('FAIL role-workspaces executable source contract: missing production paths')
    for path in missing:
        print(f'  - {path}')
    sys.exit(1)
violations = []
for path, markers in checks.items():
    source = (root / path).read_text(encoding='utf-8')
    for marker in markers:
        if marker not in source:
            violations.append(f'{path}: missing marker {marker!r}')
if violations:
    print('FAIL role-workspaces executable source contract: architecture or behavior marker missing')
    for violation in violations:
        print(f'  - {violation}')
    sys.exit(1)
print('PASS role-workspaces executable source contract: key paths and behavior markers verified')
