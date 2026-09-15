# 部署文档

可以在本地运行完整系统，也可以直接访问文末的公网演示。下面先介绍本地部署，需求和业务流程见 [SRS.pdf](SRS.pdf)。

## 一、本地部署

### 1. 环境准备

推荐使用 Docker Compose，一次启动前端、后端和 MySQL，无需在宿主机单独安装 Node.js、Java 或 MySQL。

需要：

- Git。
- Docker Desktop（macOS / Windows）或 Docker Engine + Compose v2（Linux）；启动前确认 Docker 服务正在运行。
- macOS / Linux 终端，或 Windows 的 Git Bash / WSL；自动生成配置的脚本需要 `zsh` 和 `openssl`，也可按下文手工配置。
- 能访问 GitHub、Docker 镜像仓库、npm 和 Maven 依赖仓库的网络。首次构建需要下载依赖。
- 本机端口 `18081`、`18080`、`13306` 未被其他服务占用。

先检查：

```bash
git --version
docker --version
docker compose version
```

### 2. 获取完整代码

```bash
git clone --branch main https://github.com/Fwlyii/lightweight-takeout-platform.git
cd lightweight-takeout-platform
```

后续命令除特别说明外，均在仓库根目录执行。

### 3. 生成本地配置

在已安装 zsh 和 OpenSSL 的终端执行：

```bash
zsh scripts/ensure-demo-env.sh
```

脚本会在本地 `.env` 中生成随机 `JWT_SECRET`，不会提交到 Git。已有有效密钥不会被覆盖。

没有 zsh 时，可复制根目录 `.env.example` 为 `.env`，运行 `openssl rand -hex 64`，把输出粘贴到 `.env` 的 `JWT_SECRET=` 后面，替换示例值。不要把该输出发到公开仓库或文档。

基础浏览、下单等流程无需外部 AI 密钥。可选配置按需要填入 `.env`：

| 配置 | 用途 |
| --- | --- |
| `DEEPSEEK_API_KEY` | 外部文字模型 |
| `DASHSCOPE_API_KEY`、`DASHSCOPE_BASE_URL` | 图片、语音识别服务 |
| `DASHSCOPE_VISION_MODEL`、`DASHSCOPE_SPEECH_MODEL` | 对应模型名称 |
| `VUE_APP_AMAP_KEY` | 前端地图选址；修改后需要重新构建前端 |

未配置外部能力时，相关 AI 或地图功能可能不可用，不影响验证普通点餐流程。不要填写他人的密钥。

### 4. 构建并启动

```bash
docker compose -f docker-compose.demo.yml up -d --build
docker compose -f docker-compose.demo.yml ps -a
```

首次启动过程：

1. MySQL 创建 `elm_v2` 数据库并导入表结构、演示数据。
2. `migrate` 执行订单名称快照和购物车备注迁移；成功后显示 `Exited (0)`，这是正常结果。
3. 后端使用 JDK 17 启动；前端由 Node.js 20 构建后通过 Nginx 提供服务。

查看启动日志：

```bash
docker compose -f docker-compose.demo.yml logs --tail=100 mysql migrate backend frontend
```

### 5. 访问地址

| 服务 | 地址 |
| --- | --- |
| 前端首页 | [http://localhost:18081/index](http://localhost:18081/index) |
| 后端接口根地址 | `http://localhost:18080`（不是独立网页首页） |
| Swagger 接口文档 | [http://localhost:18080/swagger-ui/index.html](http://localhost:18080/swagger-ui/index.html) |
| MySQL 调试连接 | `127.0.0.1:13306`，数据库 `elm_v2` |

前端通过 Nginx 同源代理访问后端，无需手工修改 API 地址。以上端口默认仅绑定本机。

### 6. 本地演示账号与验收

以下账号只适用于新建的本地演示数据库，初始密码均为 `Demo1234!`。在登录页选择对应角色。

| 角色 | 用户名 |
| --- | --- |
| 顾客 | `demo_user` |
| 商家 | `demo_merchant` |
| 骑手 | `demo_rider` |
| 待审核骑手 | `demo_rider_candidate` |
| 管理员 | `demo_admin` |

建议使用不同浏览器或独立浏览器会话登录不同角色，避免一个会话切换账号影响另一角色操作。

建议验收顺序：浏览商家 → 加入购物车 → 选择外送地址或自取 → 提交订单 → 模拟支付 → 商家接单与备餐 → 骑手配送（外送） → 顾客确认收货。再检查取消订单、库存不足等异常场景。跨商家商品需要分别结算。

演示支付不会向真实支付宝或微信扣款，不要进行真实转账。不得将共享演示管理员账号用于正式生产环境。

### 7. 暂停、恢复和数据注意事项

临时停止和恢复时使用：

```bash
docker compose -f docker-compose.demo.yml stop
docker compose -f docker-compose.demo.yml start
```

当前 MySQL 配置没有声明固定名称的数据卷。不要把删除容器、`down -v` 或重建数据库作为日常更新步骤；重新创建容器可能无法自动接回原有匿名数据卷。保留数据的环境应先备份数据库，再规划持久化数据卷和迁移。

初始化 SQL 仅在 MySQL 数据目录首次创建时运行。已有数据库不能靠重新启动来重复导入演示数据，也不要盲目执行 `elm_bk/db/migrations/legacy/` 中的全部脚本。上传文件保存在仓库根目录 `runtime-uploads/`，备份时一并保留。

### 8. 常见问题

| 现象 | 检查方式 |
| --- | --- |
| 提示缺少 `JWT_SECRET` | 确认根目录 `.env` 已生成且已替换示例密钥 |
| 无法连接 Docker | 打开 Docker Desktop，或启动 Docker 服务 |
| 端口占用 | 检查是否已经启动另一套项目，先停止冲突服务 |
| 镜像、npm、Maven 下载失败 | 检查网络及 Docker 代理配置，修复后重新构建 |
| 前端能打开但接口失败 | 查看 `backend`、`mysql`、`migrate` 日志，确认数据库就绪、迁移成功 |
| `migrate` 显示退出 | `Exited (0)` 表示成功；非零退出码才需要排查日志 |
| 登录失败 | 选择正确角色；确认数据库确实导入了演示种子，旧库密码可能不同 |
| 语音、图片或地图不可用 | 检查可选配置、浏览器权限与服务额度 |

### 9. 不使用 Docker 的开发方式

单独启动前后端时，需要先准备 MySQL 8、Node.js 20 和 JDK 17。

在新的本地数据库中，依次导入 `elm_bk/db/schema/elm_v2.sql`、`elm_bk/db/seeds/demo-seed.sql`、`rider-demo-seed.sql` 和 `demo-showcase-seed.sql`。再执行 `elm_bk/db/migrations/003_order_name_snapshot.sql` 和 `005_cart_order_remarks.sql`。这些步骤只用于新的本地环境，不要直接覆盖已有数据库。

根据 `elm_bk/.env.example` 设置数据库连接、数据库账号密码和 `JWT_SECRET` 环境变量，并设置 `SERVER_PORT=18080`。Maven 不会自动读取 `.env`，需要在终端或 IDE 的运行配置中设置这些变量。在 `elm_bk` 目录执行：

```bash
./mvnw spring-boot:run
```

Windows 使用 `mvnw.cmd spring-boot:run`。另开一个终端，在 `elmclient` 目录执行：

```bash
npm ci
npm run serve
```

前端开发地址以终端输出为准，默认端口为 `8080`；默认连接本机 `18080` 的后端。如果修改后端地址，可通过 `VUE_APP_API_BASE_URL` 配置前端连接。

## 二、公网访问

当前唯一正式评测入口：

**[https://elm-demo.pages.dev/index](https://elm-demo.pages.dev/index)**

该入口对应本仓库 `main` 的已发布应用版本。前端由 Cloudflare Pages 托管，通过同源代理连接云端 Railway 后端；公网访问不需要评测人员启动本机 Docker。

评测人员直接打开以上链接即可。普通顾客可通过注册入口创建账号；商家、骑手和管理员的受限流程建议使用前述本地环境验证，不在公开文档中提供公网管理账号或密钥。

公网为课程演示环境，不接入真实支付。AI、图片、语音能力取决于部署配置、外部服务及可用额度。若公网暂时不可达，可按本文第一部分独立运行完整项目。

`localhost` 地址仅供本地部署，不是公网评测入口；历史对比站和临时预览地址不用于本次提交。

### 公网部署文件的用途

`scripts/deploy-cloudflare-pages.sh` 用于发布前端。`scripts/cloudflare-pages/_worker.template.js` 是随前端一起发布的代理程序：将 `/api` 等请求转发给后端，同时处理页面刷新时的路由回退和浏览器安全响应头。它是运行代码，不是部署说明。

需要维护公网版本时，将同目录的 `.env.example` 复制为 `.env.local`，填写 `PAGES_PROJECT`、`PUBLIC_DEMO_URL` 和 `BACKEND_ORIGIN`。当前站点使用 `PAGES_PROJECT=elm-demo`、`PUBLIC_DEMO_URL=https://elm-demo.pages.dev/index`；后端源站地址由维护者在本地配置，不需要评测人员填写。

在安装 Node.js、zsh 和 curl，并通过 Wrangler 登录对应 Cloudflare 账号后，从仓库根目录执行：

```bash
./scripts/deploy-cloudflare-pages.sh main
```

设置 `BACKEND_ORIGIN` 后，脚本检查云端后端、构建前端并发布到已有 Pages 项目；这条命令不会更新 Railway 后端或数据库。不设置该值会使用本机 Docker 和临时隧道，仅适合本地演示分享。`.env.local` 只留在维护者本机，不提交到 Git。
