# 轻量外卖平台

基于 Vue 3、Spring Boot 3、MyBatis 和 MySQL 8 的课程实践项目，提供顾客、商家、骑手和管理员四个工作端。

## 1. 项目结构

```text
elmclient/                    前端工程
  src/views/                  页面
  src/components/             通用组件
  src/composables/            页面状态与交互逻辑
  src/services/               API 请求封装
  src/utils/                  通用工具与规则
  tests/                      前端回归测试
elm_bk/                       后端工程
  src/main/java/              Controller、Service、Mapper 等业务代码
  src/main/resources/         应用配置和 MyBatis 映射
  src/test/                   后端测试及隔离数据夹具
  db/schema/                  完整数据库结构
  db/seeds/                   本地演示数据
  db/migrations/              已有数据库增量迁移
report/                       课程报告 PDF 与 LaTeX 源码
deploy/                       通用部署模板
scripts/                      环境初始化、部署和检查脚本
docker-compose.demo.yml       本地运行配置
README.md                     项目说明入口
```

源码、测试和报告分开存放；使用、部署和开发说明统一维护在本文件，不额外保留操作指南和阶段日志。

## 2. 环境与本地启动

### Docker 运行

安装 Docker Desktop，在仓库根目录执行：

```bash
./scripts/ensure-demo-env.sh
docker compose -f docker-compose.demo.yml up -d --build
```

| 服务 | 本地地址 |
| --- | --- |
| 前端 | http://localhost:18081 |
| 后端 | http://localhost:18080 |
| 接口文档 | http://localhost:18080/swagger-ui/index.html |
| MySQL 调试端口 | 127.0.0.1:13306 |

停止服务：

```bash
docker compose -f docker-compose.demo.yml down
```

生成的 `.env` 仅保存在本机。更新代码不需要重置数据库；处理已有数据前先备份。

### 分开开发

前端使用 Node.js 20，在 `elmclient` 目录执行：

```bash
npm ci
npm run serve
# 生产构建
npm run build
```

前端本地开发默认连接后端 18080 端口，可用 `VUE_APP_API_BASE_URL` 覆盖；生产构建默认访问同源 API。

后端使用 JDK 17，在 `elm_bk` 目录执行：

```bash
./mvnw test
./mvnw spring-boot:run
```

先按 `elm_bk/.env.example` 将数据库连接、JWT 等配置导出为环境变量；Maven 不会自动读取 `.env`。单独启动后端时设置 `SERVER_PORT=18080`，或调整前端 API 地址与实际端口一致。不要把 Docker 内部数据库主机名直接用于宿主机进程。Windows 使用 `mvnw.cmd`。

### 本地演示账号

下列账号仅用于隔离的本地演示库，初始密码均为 `Demo1234!`，不得用于公网部署。

| 角色 | 账号 |
| --- | --- |
| 顾客 | demo_user |
| 商家 | demo_merchant |
| 骑手 | demo_rider |
| 待审核骑手 | demo_rider_candidate |
| 管理员 | demo_admin |

## 3. 功能与验收流程

- 顾客：浏览商家和菜品、购物车、地址管理、外送/自取下单、支付、取消订单、收藏、评价及个人资产。
- 商家：店铺与商品管理、库存和限购、订单接单与备餐、评价回复。
- 骑手：申请与审核、接单、到店、取餐、送达及历史任务。
- 管理员：用户管理、商家与骑手审核、配送调度及统计。
- AI 与地图：按配置启用外部服务；相关密钥见配置示例，不在源码内填写。

建议依次验证“顾客下单 → 模拟支付 → 商家接单 → 骑手配送 → 顾客确认收货”，再检查取消、库存不足、地址归属和越权访问等异常路径。跨店商品分别结算；自取无需配送地址；未支付订单不能显示支付成功。

## 4. 数据库

新数据库由 Docker 按顺序导入：

1. `elm_bk/db/schema/elm_v2.sql`
2. `elm_bk/db/seeds/demo-seed.sql`
3. `elm_bk/db/seeds/rider-demo-seed.sql`
4. `elm_bk/db/seeds/demo-showcase-seed.sql`

随后 migrate 服务执行 `003_order_name_snapshot.sql`。初始化 SQL 只在 MySQL 数据目录首次创建时运行。

已有数据库应先备份、核对表结构，再选择适用迁移。`migrations/legacy/` 保存较早的迁移，不应对整个目录盲目批量执行。脚本可能包含特定库名或基线假设，隔离测试不能只改连接参数。H2 测试夹具仍在 `src/test/resources`，不是生产迁移。

## 5. 配置与部署

后端配置包括 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`；AI 和地图使用可选密钥，参见根目录及各模块的 `.env.example`。

如使用 Cloudflare Pages，将 `deploy/cloudflare-pages/.env.example` 复制为同目录 `.env.local`，填写 `PAGES_PROJECT`，可选填写 `PUBLIC_DEMO_URL`。文件按 shell 配置读取，只能放可信的本地内容，不得提交。核对 Wrangler 登录账户和目标项目后执行：

```bash
./scripts/deploy-cloudflare-pages.sh qa-preview
# 核对预览后再发布正式分支
./scripts/deploy-cloudflare-pages.sh main
```

Pages 只托管前端和代理。默认演示模式使用本机后端与隧道，需要电脑、Docker 和网络在线。

若后端和数据库已迁到云端，在上述本地配置中设置 `BACKEND_ORIGIN`（HTTPS 源站地址，不含路径或末尾斜杠）。发布脚本会检查云端目录接口并直接连接云端，不启动本机 Docker 或隧道；云端检查失败时会终止发布，不会回退到本机。云端后端需配置数据库连接、独立 JWT 密钥及可选 AI 密钥，并将 `APP_UPLOAD_DIR` 挂载到持久化存储。已有数据库先备份再迁移；试用服务需留意额度、存储限制和到期政策。

真实域名、项目标识、密钥不记录在公开文件中；后端地址只注入临时发布产物。删除当前源码中的网址不会清除 Git 历史。

## 6. 开发与测试

### 历史功能核对（2026-09-15）

对比 `fwl-AI` 合并提交 `14617ce` 的主线父版本及更早的 `b6456ce^`：个人中心入口与头像更换在合并前的页面重写中已经遗漏，并非全部由 AI 合并引入。保留可用的 AI 点餐、识图、语音与推荐，以及后续补齐的身份隔离、地址归属和订单幂等校验，不整版回退旧代码。

| 核对项 | 当前处理 |
| --- | --- |
| 钱包、优惠券、会员、积分、消费统计 | 资产页面保留，恢复个人中心的“钱包与优惠”入口 |
| 个性化口味、忌口、品类偏好、外观主题 | 偏好页面保留，恢复个人中心入口 |
| 消息与通知 | 恢复顾客与骑手个人中心入口 |
| 更换头像 | 恢复上传；由服务端从登录态确定账号，验证图片类型与大小，不接受任意图片地址或用户 ID |
| 骑手配送中、历史配送 | 恢复个人中心快捷入口 |
| 基本资料、地址与收藏 | 保留现有受权限保护的编辑和管理流程，登录用户名与角色不通过资料接口修改 |
| 自取 | 与堂食座位无关；正常营业店铺支持自取，自取无需配送地址且不收配送费 |

顾客登录默认回首页，从 AI 工具触发登录也回首页；从购物车、订单等业务页面触发登录时仍保留安全的原页面返回。地图选址需配置 `VUE_APP_AMAP_KEY`，未配置时保留当前地点并明确提示。已存在的演示库分类修正使用 `elm_bk/db/migrations/004_demo_business_categories.sql`，执行前备份；该脚本只匹配原演示店铺及错误分类，不重置其他业务数据。

接口以 `/api` 为前缀；业务接口通常返回 `success/code/data/message`，登录和当前用户资料等既有接口保留其已约定格式，不能额外假设一层 `data`。接口文档由后端 OpenAPI 生成，路径见本地启动部分。

GET 只查询数据。商品上下架使用 `PATCH /api/foods/{id}/status`，删除使用 `DELETE /api/foods/{id}`；清空某店购物车使用 `DELETE /api/carts?businessId=...`。旧版 GET 写入入口和仅用于演示响应的 `/httpRest/*` 已移除。旧数据结构兼容接口在 OpenAPI 中标记为 deprecated，新客户端不应再引用。

未知资源返回 404，不支持的方法返回 405，不支持的请求内容类型返回 415；参数错误返回 400，认证和权限失败分别保留 401/403。调用方应展示后端错误信息，不能在失败后更新本地状态或伪造成功。

```bash
node scripts/check-repository.mjs
bash scripts/check-architecture.sh
npm --prefix elmclient test
npm --prefix elmclient run build
cd elm_bk && ./mvnw test
```

订单名称快照的独立 MySQL 回归可运行 `scripts/test-order-name-snapshot.sh`，需要 Docker；通过 `MAVEN_CACHE` 可指定 Maven 缓存目录。所有业务测试使用隔离库和合成数据，不在公开演示库创建测试订单。

Controller 负责 HTTP 参数，Service 负责业务和事务，Mapper 负责数据读写。前端页面负责展示，组合式函数管理状态，服务层封装接口。价格、权限、库存和订单状态由后端校验，不能由页面决定。

例如管理员用户管理链路为 `AdminUser.vue → useAdminAccounts → adminAccountApi → AdminAccountController → AdminAccountService`。重构应保持接口、计价和权限行为稳定；自动化测试不能替代真实浏览器验收。

## 7. 安全边界

- JWT、数据库和第三方密钥仅通过受保护配置提供，不能提交到 Git。
- API 与 WebSocket 根据当前登录身份校验权限及资源归属，不信任客户端传入的用户 ID。
- 上传限制类型和大小；订单计价、库存预占和状态变更由后端完成。
- `APP_DEMO_ENABLED` 控制模拟支付、充值和免费会员。生产必须关闭，并接入支付平台的服务端回调验签、金额校验和幂等处理。
- 不把隐藏网址当作访问控制。公网部署使用独立账户、HTTPS、限流、审计和备份；不可开放共享管理员账号。

## 8. 课程报告与素材

课程报告：[PDF](report/tjumain.pdf)。源码位于 `report/source/`，在该目录执行 `make pdf`，生成 `report/tjumain.pdf`。正文在 `body/srs/`，图源在 `figures/srs/src/`；重新生成图需要 Python、Pillow 和中文字体，可通过 `SRS_FONT_REGULAR`、`SRS_FONT_BOLD` 指定字体。

图片来源和许可证随素材保存：[菜品图片](elmclient/public/images/foods/SOURCES.txt)、[商家图片](elmclient/public/images/merchants/SOURCES.txt)。这些是署名信息，不是额外的操作文档。
