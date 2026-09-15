# 轻量外卖平台

基于 Vue 3、Spring Boot 3、MyBatis 和 MySQL 8 的课程实践项目，提供顾客、商家、骑手和管理员四个工作端。

## 评测入口

- 提交分支：`main`，包含完整项目源码、测试和文档。
- [需求规格说明书（SRS）](docs/SRS.pdf)
- [部署文档：本地部署与公网访问](docs/DEPLOYMENT.md)
- [公网演示网站](https://elm-demo.pages.dev/index)

## 目录结构

```text
elmclient/                    Vue 3 前端
  src/views/                  四个角色的页面
  src/components/             公共组件
  src/composables/            页面状态与交互逻辑
  src/services/               API 请求封装
  src/router/                 路由与访问控制
  src/utils/                  通用工具与规则
  tests/                      前端回归测试
elm_bk/                       Spring Boot 后端
  src/main/java/              Controller、Service、Mapper 等业务代码
  src/main/resources/         应用配置和 MyBatis 映射
  src/test/                   后端测试及隔离数据
  db/schema/                  完整数据库结构
  db/seeds/                   本地演示数据
  db/migrations/              已有数据库增量迁移
docs/                         评测文档
  SRS.pdf                     需求规格说明书
  DEPLOYMENT.md               本地部署流程与公网链接
  source/                     SRS 的 LaTeX 源码及配图
deploy/                       部署模板
scripts/                      环境初始化、部署和检查脚本
docker-compose.demo.yml       本地前端、后端、数据库运行配置
```

## 主要功能

- 顾客：浏览商家与菜品、购物车、地址管理、外送或自取、支付、取消订单、收藏、评价、个人资产。
- 商家：店铺与商品管理、库存和限购、接单与备餐、评价回复。
- 骑手：申请与审核、接单、到店、取餐、送达及历史任务。
- 管理员：用户管理、商家与骑手审核、配送调度及统计。
- 智能辅助：文字、图片、语音点餐及推荐，外部能力按配置启用。

跨店商品分别结算；自取无需配送地址且不收配送费。金额、库存、权限和订单状态以服务端校验为准。支付宝、微信为演示支付入口，尚未接入真实支付渠道，不要进行真实转账。

## 开发与测试

完整启动步骤、依赖、端口、演示账号和排错方式见[部署文档](docs/DEPLOYMENT.md)。

安装 Node.js 20 和 JDK 17 后，可分别运行：

```bash
node scripts/check-repository.mjs
bash scripts/check-architecture.sh
npm --prefix elmclient ci
npm --prefix elmclient test
npm --prefix elmclient run build
cd elm_bk
./mvnw test
```

业务测试使用隔离库和合成数据，不应在公网演示数据库中创建测试订单。自动化测试不能代替完整浏览器验收。

Controller 负责 HTTP 参数，Service 负责业务和事务，Mapper 负责数据读写。接口文档由后端 OpenAPI 生成。接口通常返回 `success/code/data/message`，登录和当前用户接口保留各自约定格式。

## 文档维护

需求正文位于 `docs/source/body/srs/`。安装包含中文支持的 TeX Live（XeLaTeX、latexmk）后，在 `docs/source` 执行 `make pdf`，输出 `docs/SRS.pdf`。配图及图源均保留在 `docs/source/figures/`。

README、部署文档、需求文档及其源码不包含具体组号、组员姓名或学号。更新 SRS 后应同时提交源码和生成的 PDF，并检查 PDF 元数据。

图片来源与许可证：[菜品图片](elmclient/public/images/foods/SOURCES.txt)、[商家图片](elmclient/public/images/merchants/SOURCES.txt)。第三方素材署名不属于项目组员信息，应保留。

## 安全说明

- JWT、数据库和第三方密钥通过本地配置或部署环境变量提供，不提交真实密钥。
- API 和 WebSocket 校验身份及资源归属，不能用隐藏网址代替权限控制。
- 本地演示账号与数据库默认密码仅供隔离演示，不用于真实生产环境。
- `APP_DEMO_ENABLED` 控制模拟支付、充值和免费会员；真实生产必须关闭，并接入支付平台服务端验证。
- 数据库升级前先备份，不批量执行历史迁移，也不为更新代码而重置业务数据。
