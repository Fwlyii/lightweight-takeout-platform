# lcw：个人资料、地址和收藏交接

## 这次提交做了什么

本次对应 P28–P31。P28 先提交实际失败的接口测试；P29 实现功能让原测试通过；P30 整理公共逻辑并加回归检查；P31 通过 PR 合入主线。

测试会保留在 main 中，后续每次改代码都可以再运行，用来发现旧功能被意外改坏。不要在合并后删除测试，也不要直接用沙盘整目录覆盖 main：真实仓库里已经增加了接口测试、权限检查和数据库迁移。

## 代码分工怎么理解

- controller 是入口：收取请求、校验字段、调用服务、返回结果，不决定地址属于谁。
- AddressServiceImpl 管理地址生命周期：当前用户身份、默认地址规则、编辑和删除。
- ProfileUpdateService 只允许修改姓名、电话、邮箱和性别，不允许借此修改权限、账号或密码。
- MerchantInteractionService 管理收藏/点赞关系；设置明确的 true/false，重复提交不会“又翻转一次”。
- AccountWriteLock 是同一用户写操作的数据库锁，必须在事务中使用。保护的是服务端并发，不是浏览器按钮状态。
- BusinessMapper 的 BusinessSearchColumns 统一评分、销量等查询；BusinessPresentationService 统一补充标签。收藏页不保存另一份评分。
- 前端视图负责展示；profileApiClient 负责请求格式；最终权限和业务规则仍由后端负责。

## 自己运行测试

在 lightweight-takeout-platform 根目录执行：

```sh
cd elm_bk
mvn test
cd ../elmclient
npm ci
npm test
npm run build:profile
```

需要 JDK17、Maven 和 Node。后端测试自动创建独立 H2 内存数据库，不需要导入数据到你正在使用的 MySQL。

## 单独看本次页面

不必等全部功能完成。开两个终端：

第一个终端，在 elm_bk 中启动测试后端：

```sh
PROFILE_PREVIEW_PORT=18084 mvn spring-boot:test-run -Dspring-boot.run.main-class=com.tju.elm_bk.profile.ProfilePreviewApplication
```

这条命令将预览后端设为18084端口，避免与其他程序冲突。Docker运行时也可以不设置该变量，将容器默认8080映射到本机18084。

第二个终端，在 elmclient 中：

```sh
npm run serve:profile
```

打开 http://localhost:18085/myInformation，使用 preview_user / Study2026!。这只是本地测试账号；数据随测试后端停止而消失。前端预览默认连接18084后端端口。

## 接手前要知道的限制

- 如连接已有真实开发数据库，先备份，再执行一次002_default_address.sql；不要在已有数据的数据库直接重新导入带DROP TABLE的新建表脚本。
- 地图搜索和地理编码不在本次范围，地址为用户填写的详细文字。
- 修改头像、修改密码、注销账号不属于本次资料编辑接口；头像沿用注册时上传的图片或默认头像。
- 完整前端 build 仍有后续页面/组件依赖，不要把 build:profile 成功误认为完整 demo 已完成。
- 下一段按计划是 fwl 的 P32–P35。新增接口应在最新 main 上开发，避免覆盖本次服务和测试。
