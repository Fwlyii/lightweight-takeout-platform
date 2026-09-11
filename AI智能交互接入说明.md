# AI 智能交互接入说明

更新日期：2026-09-05

## 已实现能力

- 结构化文字助手：`POST /api/v1/assistant/messages` 自动识别推荐、订单、规则和通用意图。
- 真实商品推荐：只读取已审核商家中上架、有库存的商品；支持自然语言关键词、文本预算、显式预算、忌口、品类、口味、辣度、本人完成订单购买历史和平台完成订单销量。
- 冷启动推荐：无关键词或无个人偏好时按完成订单销量优先，不生成虚构商家、商品、价格、评分或时效。
- 图片识菜：校验 MIME、文件签名和大小后调用 Qwen-VL，模型只返回识别关键词，再由真实商品推荐服务生成候选。
- 语音点单：浏览器录音或上传音频，调用 Qwen-ASR；草稿可编辑商品关键词、数量、规格和预算。
- 受控购物车写入：AI、图片和语音只返回候选；用户点击商品卡片后才调用 `POST /api/v1/cart/items`，后端重新校验账号、商家状态、上下架、库存和限购。
- 安全配送导航：`GET /api/v1/delivery-tasks/{id}/navigation` 校验骑手资质、任务归属和当前阶段后返回高德 URI，不向前端暴露地图 Key。
- 会话安全：请求体中的用户 ID 不被信任；会话读取、续聊和删除均校验当前账号，删除 SQL 同时包含记录 ID、用户 ID 和未删除条件。
- 稳定性：外部 AI 请求总超时 10 秒、最多重试 1 次；连续失败 3 次后熔断 30 秒。
- 本地演示：启用 Spring profile `ai-mock` 可在没有百炼 Key 时走固定图片/语音结果，最终商品候选仍来自数据库。

图片和音频不会写入服务器磁盘。Spring multipart 内存阈值为 10 MB，业务层限制图片 5 MB、音频 7 MB，并检查真实文件签名。

## 环境变量

```bash
export DEEPSEEK_API_KEY="your-deepseek-key"
export DEEPSEEK_BASE_URL="https://api.deepseek.com"
export DEEPSEEK_MODEL="deepseek-v4-flash"
export DEEPSEEK_THINKING_ENABLED="false"

export DASHSCOPE_API_KEY="your-dashscope-key"
export DASHSCOPE_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
export DASHSCOPE_VISION_MODEL="qwen3-vl-flash"
export DASHSCOPE_SPEECH_MODEL="qwen3-asr-flash"
```

未配置 `DEEPSEEK_API_KEY` 时，文字助手自动使用本地规则降级服务。未同时配置 `DASHSCOPE_API_KEY` 和 `DASHSCOPE_BASE_URL` 时，图片和语音能力保持禁用。

无外部 Key 的联调方式：

```bash
export SPRING_PROFILES_ACTIVE=ai-mock
```

Docker 演示可在仓库根目录创建本地 `.env`，然后执行：

```bash
docker compose -f docker-compose.demo.yml up -d --build
```

不要把包含真实密钥的 `.env` 提交到 Git。

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/assistant/capabilities` | 查询文字、图片和语音能力状态 |
| POST | `/api/v1/assistant/messages` | 结构化文字消息，推荐意图返回真实商品卡片 |
| POST | `/api/v1/recommendations` | 自然语言、预算与授权偏好推荐 |
| POST | `/api/v1/dish-recognitions` | Multipart 字段 `image`，最大 5 MB |
| POST | `/api/v1/voice-order-drafts` | Multipart 字段 `audio`，最大 7 MB |
| POST | `/api/v1/cart/items` | 用户确认后加入购物车，JSON 为 `foodId`、`quantity` |
| GET | `/api/v1/delivery-tasks/{id}/navigation` | 当前骑手、当前配送阶段的导航信息 |

原有 `/api/ai/chat`、聊天历史和 `/api/ai/chat/recommendations` 保留兼容，但新页面使用 `/api/v1/assistant/messages`。

## 请求示例

```json
{
  "message": "推荐30元以内的清淡牛肉面",
  "sessionId": null,
  "budget": null,
  "usePreferences": true
}
```

推荐响应中的 `candidates` 只包含数据库商品 ID、当前价格、图片、商家和可解释推荐原因。`needConfirmation=true` 表示前端必须等待用户点击，不允许模型直接修改购物车或创建订单。

## 降级与错误行为

- 外部模型未配置：文字查询、真实推荐、订单查询和购物车确认仍可使用。
- 图片或语音未配置：能力接口返回不可用，前端禁用对应按钮。
- 外部请求超时、限流或连续失败：返回明确错误；不会自动创建订单或修改购物车。
- 模型识别无对应商品：保留识别关键词，候选为空，不补造商品。
- 麦克风权限被拒绝：可上传已有音频或切换到文字推荐。
- 导航任务已完成、取消、重派或不属于当前骑手：后端拒绝返回地址。

## 验收

1. 输入“推荐30元以内的清淡牛肉面”，确认返回商品均不超过 30 元，且来自已审核商家和当前库存。
2. 配置忌口与辣度后检查过滤和排序；新账号检查结果按真实完成订单销量排序。
3. 上传合法 JPG、PNG、WebP 和音频，再验证伪造扩展名、错误签名及超限文件会被拒绝。
4. 录制“帮我来两杯大杯少冰奶茶，预算30元”，检查数量、规格、预算和候选均可编辑。
5. 确认候选出现时购物车未变化，只有点击“加入”后才写入，并再次验证库存与限购。
6. 使用另一个账号的会话 ID、历史 ID 和配送任务 ID，确认均被拒绝。
7. 连续模拟三次外部 AI 失败，确认后续请求短暂熔断；30 秒后允许恢复尝试。
8. 移除真实 API Key 并启用 `ai-mock`，确认图片和语音流程可联调，候选仍取自数据库。
