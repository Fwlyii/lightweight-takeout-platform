from pathlib import Path
import sys

root = Path(__file__).resolve().parents[2]
checks = {'elm_bk/src/main/java/com/tju/elm_bk/controller/AiChatController.java': ['"/recommendations"', 'budget', 'AiRecommendationVO'], 'elm_bk/src/main/java/com/tju/elm_bk/service/impl/MockAiChatServiceImpl.java': ['联系人工客服', 'generateMockResponse'], 'elmclient/src/views/AiChat.vue': ['recommendations', 'smartBudget']}
missing = [path for path in checks if not (root / path).is_file()]
if missing:
    print('FAIL ai-operations executable source contract: missing production paths')
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
    print('FAIL ai-operations executable source contract: architecture or behavior marker missing')
    for violation in violations:
        print(f'  - {violation}')
    sys.exit(1)
print('PASS ai-operations executable source contract: key paths and behavior markers verified')
