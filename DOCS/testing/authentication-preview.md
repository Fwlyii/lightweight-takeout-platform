# 注册与分端登录：本地联调说明

仓库：`Fwlyii/lightweight-takeout-platform`。
本入口用于其他业务页面尚未提交时，独立验证账号功能；不是成品 Demo，也不需要公网部署。

## 1. 运行自动化测试

在仓库根目录打开终端。安装了 JDK 17 时：

```bash
cd elm_bk
./mvnw test
```

没有本机 Java，但安装了 Docker 时，在仓库根目录运行：

```bash
docker run --rm \
  -v "$PWD/elm_bk":/workspace \
  -v lcw-auth-maven-cache:/root/.m2 \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-17 mvn -B test
```

Maven 首次会下载依赖。测试使用 H2 内存数据库，不连接本机 MySQL。
测试结束应看到 `Tests run: 73, Failures: 0, Errors: 0, Skipped: 0` 和 `BUILD SUCCESS`。

再开一个终端，在仓库根目录运行前端测试（需要 Node.js 22.7+，建议与组员统一使用 Node.js 24）：

```bash
cd elmclient
npm ci
npm run test:auth
npm run build:auth
```

前端测试当前为 15 项；`build:auth` 只构建账号页面，输出到不进入 Git 的 `dist-auth`。
Node 可能提示 `.js` 模块格式自动识别，这不是测试失败；不能直接给整个 package.json 加 `type: module`，
因为现有 Vue CLI 配置仍使用 CommonJS。

## 2. 启动真实账号联调后端

先确保 Docker 正在运行。在仓库根目录打开一个终端，执行：

```bash
docker run --rm --name lcw-auth-preview \
  -p 127.0.0.1:18082:8080 \
  -v "$PWD/elm_bk":/workspace \
  -v lcw-auth-maven-cache:/root/.m2 \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-17 \
  mvn -B spring-boot:test-run \
  -Dspring-boot.run.main-class=com.tju.elm_bk.auth.AuthPreviewApplication
```

看到 `Local authentication preview ready` 表示已经启动。
这个终端保持运行；它不是卡住了，而是在等待网页请求。
如果提示同名容器已存在，先确认是否已有本入口在运行，不要删除其他同学的容器。

`AuthPreviewApplication` 位于 `src/test/java`，使用测试 classpath 中的 H2。
它在启动参数里固定内存数据库，并生成四个测试账号，避免误连现有 Demo 数据库。
上传图片放在容器临时目录；关闭容器后，内存账号和上传文件都不保留。
这些测试代码不放入正式应用 JAR。

## 3. 启动前端

另开一个终端，在仓库根目录运行：

```bash
cd elmclient
npm run serve:auth
```

打开 [本地登录页](http://localhost:18083/login)。
前端只监听本机，默认连接 `http://localhost:18082`，不会访问公网 Demo。
这不是 18081 的成品页面，也不是 28081 的沙盘成品预览。

| 选择的端 | 测试账号 | 测试密码 |
| --- | --- | --- |
| 用户 | `preview_user` | `Study2026!` |
| 商家 | `preview_merchant` | `Study2026!` |
| 骑手 | `preview_rider` | `Study2026!` |
| 管理员 | `preview_admin` | `Study2026!` |

这些只属于隔离联调入口，不是成品 Demo 的账号或生产凭据。

按下面顺序检查：

1. 在用户端输入骑手账号，应收到“请选择骑手端登录”，不能进入账号页面。
2. 切换骑手端后用同一账号登录，应看到真实的用户名、手机号、登录端和账号身份。
3. 退出后再访问 `/auth/session`，应回到登录页。
4. 注册一个新账号，头像和邮箱留空；随后使用手机号登录，应该看到默认头像。
5. 普通用户从商家或骑手端登录，只获得申请会话。联调页会同时显示“登录端”和“账号身份”，
   不提供店铺、配送等工作权限；实际入驻业务在后续模块实现。

登录、注册两个页面直接复用正式组件；`src/auth-preview/Session.vue` 只是可见的验收结果页。
它会用真实令牌调用 `/api/user`，没有伪造成功数据。正式登录组件不依赖该验收页。

## 4. 使用旧 MySQL 数据库时如何升级

本步骤针对后续成员自己的开发数据库，不用于上述内存联调入口。

- 新建数据库：最新 `elm_bk/elm_v2.sql` 已含两个唯一索引，不需要再执行迁移。
- 已有数据库：先备份，并检查 `users.username` 和 `person.phone` 是否重复。
  `elm_bk/db/migrations/001_account_uniqueness.sql` 开头给出了只读检查 SQL。
- 如果存在重复记录，先和负责数据的同学确认保留哪条，不能自动删除或重新生成账号。
- 没有重复、且两个索引尚不存在时，在明确选中的开发数据库执行该迁移文件一次。
  迁移不会随应用自动执行；重复执行会报告索引已存在，不要据此删除索引。
- 软删除账号仍保留其用户名和手机号，避免旧身份被重新占用。

唯一约束负责处理“两个请求同时注册”的最后一道防线，应用里的提前查询只负责友好提示。
数据库异常转换为 409 后，整笔注册事务仍会回滚。

## 5. 停止联调入口

在前端终端按 `Ctrl+C`。后端可在另一个终端执行：

```bash
docker stop lcw-auth-preview
```

该命令只停止本说明创建的临时后端；`--rm` 会自动清理容器及临时数据。
它不会停止原本的成品 Demo。
