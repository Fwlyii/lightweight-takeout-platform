# 轻量外卖平台

这是一个面向校园点餐场景的外卖系统，包含顾客、商家、骑手和管理员四个端。顾客可以浏览店铺、选择外送或到店自取；商家负责接单和备餐，骑手完成配送，管理员处理账号审核和平台管理。系统还提供 AI 辅助点餐、优惠券、积分和消息通知等功能。

前端使用 Vue 3，后端使用 Spring Boot 3 和 MyBatis，数据库使用 MySQL 8。

[本地部署](docs/DEPLOYMENT.md) · [需求规格说明书](docs/SRS.pdf)

## 先运行起来

推荐使用 Docker Compose。安装并启动 Docker 后，在仓库根目录执行：

```bash
# 生成本地配置，需要 zsh 和 OpenSSL
zsh scripts/ensure-demo-env.sh

# 构建并启动前端、后端和数据库
docker compose -f docker-compose.demo.yml up -d --build
```

启动完成后，打开 [http://localhost:18081/index](http://localhost:18081/index)。首次构建需要下载依赖，可能需要等待几分钟。

没有 zsh 的配置方法、各角色演示账号和常见问题都放在[部署文档](docs/DEPLOYMENT.md)中。

## 可以体验什么

| 角色 | 主要功能 |
| --- | --- |
| 顾客 | 浏览和搜索商家、购物车、地址管理、外送与自取、支付、订单跟踪、收藏和评价 |
| 商家 | 店铺资料、菜品与库存、接单、备餐、评价回复 |
| 骑手 | 入驻申请、接单、到店取餐、配送与历史任务 |
| 管理员 | 用户管理、商家与骑手审核、配送调度和统计 |

建议先走一遍“顾客下单 → 模拟支付 → 商家接单 → 骑手配送 → 顾客确认收货”，再查看自取、取消订单和库存不足等情况。多角色操作时，可以使用不同浏览器或独立会话。

AI 点餐支持文字、图片和语音入口，相关外部服务需要配置密钥。支付宝和微信目前是模拟支付，不会发起真实扣款。

## 仓库导航

```text
elmclient/                    前端页面、组件和测试
elm_bk/                       后端接口、业务逻辑、数据库脚本和测试
docs/
  SRS.pdf                     需求规格说明书
  DEPLOYMENT.md               本地部署说明
  source/                     需求文档的 LaTeX 源码和配图
scripts/
  cloudflare-pages/           部署代理程序和配置示例
  tests/                      部署脚本测试
  *.sh / *.mjs                启动、部署和检查工具
docker-compose.demo.yml       本地运行配置
```

### 从哪里开始读代码

如果想了解点餐流程，可以先看前端的 [BusinessInfo.vue](elmclient/src/views/BusinessInfo.vue)、[Cart.vue](elmclient/src/views/Cart.vue) 和 [Payment.vue](elmclient/src/views/Payment.vue)，再看后端的 [OrderController.java](elm_bk/src/main/java/com/tju/elm_bk/controller/OrderController.java) 和 [OrderSubmissionService.java](elm_bk/src/main/java/com/tju/elm_bk/service/OrderSubmissionService.java)。

前端的 `src/views/` 按页面组织，公共组件在 `src/components/`，接口封装在 `src/services/`，路由和登录访问控制在 `src/router/`。`auth-preview` 和 `profile-preview` 是登录、资料功能的独立联调入口，正常启动不用进入这些目录。

后端按 Controller、Service、Mapper 分层，分别处理 HTTP 请求、业务逻辑和数据库访问。数据库结构在 `elm_bk/db/schema/`，本地演示数据在 `db/seeds/`，已有数据库的更新脚本在 `db/migrations/`。

需求文档按功能和业务流程展开，适合对照代码阅读；其中的角色分析、订单状态和用例图可以帮助理解四个端之间的关系。

## 开发和测试

前端需要 Node.js 20：

```bash
cd elmclient
npm ci
npm run serve
```

后端需要 JDK 17 和可用的 MySQL。在 `elm_bk` 目录运行 `./mvnw spring-boot:run`；Windows 使用 `mvnw.cmd`。分开运行时需要配置数据库连接和 JWT 密钥，具体步骤见[部署文档](docs/DEPLOYMENT.md)。

在仓库根目录可以运行以下检查：

```bash
# 前端测试
npm --prefix elmclient test

# 后端测试
cd elm_bk
./mvnw test
cd ..

# 目录、链接和代码约定检查
node scripts/check-repository.mjs
bash scripts/check-architecture.sh

# 部署脚本测试（需要 zsh）
node --test scripts/tests/*.test.mjs
```

后端测试使用独立测试数据库。修改数据库结构前请先备份，并查看对应迁移脚本的适用条件。

## 修改文档

需求正文在 `docs/source/body/srs/`，配图在 `docs/source/figures/`。安装支持中文的 TeX Live、XeLaTeX 和 latexmk 后，在 `docs/source` 运行 `make pdf` 即可重新生成 `docs/SRS.pdf`。

本地配置文件 `.env` 不会提交到仓库。数据库密码、JWT 和外部服务密钥请保存在本地或部署平台的环境变量中。
