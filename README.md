# 民宿管理系统

项目按你的要求拆成两个目录：

- `springboot/`：Spring Boot 3.5 后端
- `vue/`：Vue 3 + Vite 前端

## 核心设计

这版重点不是“一个民宿只能住一个订单”的简化模型，而是：

- 一个民宿 `Homestay`
- 对应多个真实房间 `Room`
- 用户下单时先按入住/退房日期查询可用房号
- 一个订单 `BookingOrder` 可以绑定多个房号 `BookingOrderRoom`

这样房东就能自行维护房源下的房间库存，用户也能按房号选择，不会出现传统成品里“一旦有人预订整套民宿就不可再订”的问题。

## 已完成模块

后端：

- 注册登录
- JWT 鉴权
- 首页数据
- 房源分页搜索
- 房源详情
- 按日期查询可订房号
- 下单、模拟支付、取消、完成
- 收藏
- 评论与房东回复
- 后台看板
- 房源发布
- 订单管理
- 用户管理
- 评论管理
- 订单 CSV 导出
- 演示数据初始化

前端：

- 游客首页
- 房源详情页
- 用户中心
- 后台管理页
- 图表展示
- 房源发布表单

## 数据库

后端默认连接：

- Host: `106.53.10.60:3306`
- DB: `homestay`
- User: `homestay`
- Password: `homestay`

配置文件在：

- `springboot/src/main/resources/application.properties`

建表脚本在：

- `springboot/src/main/resources/sql/homestay.sql`

## 演示账号

- 管理员：`admin / admin123`
- 房东：`host / host123`
- 游客：`user / user123`

## 启动方式

后端：

```bash
cd springboot
./mvnw spring-boot:run
```

前端：

```bash
cd vue
npm install --legacy-peer-deps
npm run dev
```

默认访问：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`

## 说明

这版已经可以作为课程设计基础工程直接往下写论文、补截图和继续扩展。如果你下一步要，我可以继续帮你补：

- 论文开题/摘要/目录
- 用例图、ER 图、时序图
- 接口文档
- 答辩 PPT 文案
- 真正的后台菜单拆分与更多页面
