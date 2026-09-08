from pathlib import Path
import sys

root = Path(__file__).resolve().parents[2]
checks = {'elm_bk/src/main/java/com/tju/elm_bk/controller/AssetController.java': ['"/welcome-coupon"', '"/ledger"'], 'elmclient/src/utils/theme.js': ['THEME_OPTIONS', 'document.documentElement.dataset.theme'], 'elmclient/src/views/Notifications.vue': ['realtimeService', 'notifications']}
missing = [path for path in checks if not (root / path).is_file()]
if missing:
    print('FAIL assets-preferences executable source contract: missing production paths')
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
    print('FAIL assets-preferences executable source contract: architecture or behavior marker missing')
    for violation in violations:
        print(f'  - {violation}')
    sys.exit(1)
print('PASS assets-preferences executable source contract: key paths and behavior markers verified')
