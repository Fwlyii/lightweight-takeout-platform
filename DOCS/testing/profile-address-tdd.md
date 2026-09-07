# 个人资料、地址与收藏：开发与验收记录

## 功能目标

用户可以修改自己的资料，新增、修改、删除收货地址并设置默认地址，收藏或取消收藏已审核商家。收藏与首页的商家评分、销量和推荐标签使用同一后端查询及规则，不复制另一套计算公式。

## 接口约定

- GET /api/user 沿用登录模块的当前资料接口；PUT /api/user 修改自己的姓名、性别、电话、邮箱。账号、权限、密码和任意头像 URL 不属于此接口的可修改字段。
- GET/POST /api/addresses/me 查询/新增自己的地址；GET/PUT/DELETE /api/addresses/{id} 查询/修改/删除自己的地址；PUT /api/addresses/{id}/default 设置默认地址。
- 第一条地址自动成为默认地址。更换默认地址后只保留一个默认值；删除默认地址后自动选择剩余地址中的第一条。默认值保存在数据库，不依赖某台设备的浏览器缓存。
- GET /api/merchant/interaction/collections/me 查询自己的收藏；GET /api/merchant/interaction/status/me 查询对指定商家的互动状态；POST /api/merchant/interaction/update 用明确的布尔值设置收藏/点赞，重复设置不产生重复记录。
- 地址、收藏的所属人从登录态解析；用户不能通过 userId、customer.id 等请求字段替他人操作。不存在和不属于自己的地址均返回404，避免泄露地址是否存在。

## 测试先行

ProfileAddressJourneyTest 在新业务接口实现之前加入，使用真实 Spring/MockMvc/MyBatis 和独立 H2 内存数据库。测试数据库中的 is_default 是本功能拟增加的字段，生产建表和迁移在实现阶段提供。

该阶段只编写可运行的测试和验收说明，不将生产编译错误算作功能 RED。后续会记录实际 RED、GREEN 和重构测试结果。

### RED 实测（2026-09-07）

运行 `mvn -Dtest=ProfileAddressJourneyTest test`：生产及测试代码编译成功，Spring/H2 启动成功；18 项验收断言失败，0 执行错误、0 跳过。当前 PUT /api/user 尚不存在，返回405；地址及互动路由尚不存在，由现有通用异常处理返回500，因此未满足测试要求的正常/校验/权限状态。完整摘要见 profile-address-red.txt。

### GREEN 实测（2026-09-07）

实现后，相同的 ProfileAddressJourneyTest 18 项全部通过，没有修改原有断言来迁就实现。后端全量127项测试通过、0失败/错误/跳过；前端 `npm run build:profile` 构建成功。该入口使用本次生产页面和真实接口，隔离其他成员尚未完成的路由，并不代表完整 demo 的前端构建已经通过。

已实现的后端写操作使用数据库事务。AccountWriteLock 对当前用户记录加行锁，避免同一个用户并发创建地址时出现多个默认值、并发收藏时产生重复记录。不是仅靠前端禁用按钮。

已有数据库先备份，再执行一次 elm_bk/db/migrations/002_default_address.sql；新数据库直接使用 elm_v2.sql。此次未操作演示数据库。
