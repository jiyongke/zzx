# 橙心助农 · 农产品溯源电商平台

基于 **Spring Boot 2.7 + Vue 3** 的农产品溯源电商平台，实现"从田间到餐桌"的全链路农产品溯源 + 电商交易闭环。

## 功能模块

| 模块 | 说明 | 角色 |
|---|---|---|
| 农产品溯源（核心） | 溯源批次管理、八大环节（选种/施肥/防治/采收/加工/质检/仓储/物流）录入、二维码/批次号溯源查询、溯源链发布 | 农户录入，消费者查询，管理员监管 |
| 电商交易 | 商品浏览/搜索/分类、购物车、下单、模拟支付、发货、确认收货、取消/售后 | 消费者、物流、管理员 |
| 多角色权限 | JWT 登录鉴权 + 角色注解，管理员/农户/物流/消费者四类角色，接口级权限控制 | 全部 |
| 数据统计可视化 | 管理端 ECharts 看板：近7日销售趋势、分类销售占比、溯源批次状态、最新订单 | 管理员 |

## 技术栈

- **后端**：Spring Boot 2.7.18（Java 8）、MyBatis-Plus 3.5.3、JWT（jjwt 0.9.1）、MySQL 5.6/8.x
- **前端**：Vue 3、Vite 5、Element Plus、Pinia、Vue Router 4、Axios、ECharts 5

## 项目结构

```
orange-farm/
├── backend/                 # Spring Boot 后端
│   └── src/main/
│       ├── java/com/orange/farm/
│       │   ├── common/      # 统一返回 Result、全局异常、@RequireRole 注解
│       │   ├── config/      # 跨域、MyBatis-Plus 分页
│       │   ├── controller/  # 认证/用户/分类/商品/购物车/订单/溯源/统计 8 组接口
│       │   ├── dto/ vo/     # 入参 DTO / 出参 VO
│       │   ├── entity/      # 9 张表对应实体
│       │   ├── interceptor/ # JWT 登录 + 角色权限拦截器
│       │   ├── mapper/      # MyBatis-Plus Mapper（含统计 SQL）
│       │   ├── service/     # 业务逻辑
│       │   └── util/        # JWT / MD5 / 登录上下文
│       └── resources/application.yml
├── frontend/                # Vue 3 前端
│   └── src/
│       ├── api/             # axios 封装 + 全部接口
│       ├── router/          # 路由（登录/角色守卫）
│       ├── store/           # Pinia 用户状态
│       └── views/           # 消费者端 + 管理端页面
├── sql/
│   └── orange_farm.sql      # 建库建表 + 演示数据
└── README.md
```

## 快速启动

### 1. 初始化数据库

```bash
# 方式一：命令行导入
mysql -uroot -p < sql/orange_farm.sql

# 方式二：Navicat / DataGrip 等工具直接执行 sql/orange_farm.sql
```

脚本会自动创建 `orange_farm` 数据库、9 张表并插入演示数据。

### 2. 启动后端

1. 修改 `backend/src/main/resources/application.yml` 中的数据库账号密码（默认 `root / jyk123123`，端口 8080）。
2. 启动：
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. 后端启动成功访问 `http://localhost:8080`。

> 注：本项目在 `pom.xml` 中显式固定了 Java 8 编译参数；若本机 Maven 全局配置了 jdk-17 profile，也不会再影响编译。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:3000`（开发服务器已将 `/api` 代理到后端 8080）。

## 演示账号（密码均为 123456）

| 账号 | 角色 | 用途 |
|---|---|---|
| admin | 管理员 | 数据看板、商品/分类/订单/用户/溯源全量管理 |
| farmer01 / farmer02 / farmer03 | 农户 | 新建溯源批次、录入溯源环节、发布溯源链 |
| logistics | 物流 | 订单发货 |
| consumer01 | 消费者 | 购物、下单、支付、溯源查询 |

## 核心流程演示

1. **溯源链闭环**：登录 `farmer01` → 溯源管理 → 新建批次 → 管理环节逐条录入（种植→施肥→…→物流）→ 发布 → 登录 `consumer01` 或在未登录状态访问"溯源查询"，输入批次号（如 `TR20260901001`）查看完整溯源链。
2. **电商交易闭环**：消费者选商品 → 加入购物车 → 结算（选地址）→ 模拟支付 → 物流账号发货 → 消费者确认收货。
3. **管理看板**：管理员登录 → 数据看板，查看 ECharts 销售趋势、分类占比、批次状态。

## 接口清单（摘要）

| 模块 | 接口 |
|---|---|
| 认证 | POST /api/auth/register、POST /api/auth/login、GET /api/auth/info |
| 商品 | GET /api/product/list、GET /api/product/detail/{id}、GET /api/product/hot |
| 溯源 | GET /api/trace/query?code=、GET /api/trace/batch/{id}、POST /api/trace/batch、POST /api/trace/node、PUT /api/trace/batch/{id}/publish |
| 购物车 | GET/POST/PUT/DELETE /api/cart/** |
| 订单 | POST /api/order/create、PUT /api/order/{id}/pay、/ship、/confirm、/cancel、POST /api/order/{id}/refund-apply（申请售后）、PUT /api/order/admin/{id}/refund-handle（审核退款） |
| 管理 | GET /api/admin/dashboard、/api/admin/report/daily（日报）、/api/admin/report/weekly（周报）、/api/admin/audit/list（操作日志）、/api/admin/user/list、/api/product/admin/list、/api/order/admin/list、/api/trace/admin/list |

订单状态：PENDING 待支付 → PAID 已支付 → SHIPPED 已发货 → COMPLETED 已完成；PENDING 可 CANCELLED；PAID/SHIPPED 可申请售后 → REFUNDING（审核）→ REFUNDED 已退款（销量回滚）或驳回恢复原状态。

## 常见问题

- **8080 被占用**：修改 `application.yml` 中 `server.port`，并同步修改 `frontend/vite.config.js` 的代理 target。
- **MySQL 5.6 中文乱码**：导入脚本已指定 `utf8mb4`，若仍乱码请确认连接串带 `characterEncoding=utf8`（已配置）。
- **Java 版本**：项目基于 Java 8 构建（Spring Boot 2.7），使用 JDK 8/11/17 均可运行。

## 自动化验证

项目根目录附带端到端验证脚本 `verify_e2e.py`，覆盖"登录 → 溯源查询 → 购物车 → 下单 → 模拟支付 → 管理看板 → 物流发货 → 确认收货"完整业务链路：

```bash
# 前后端启动后执行（结果全通过即链路正常）
python verify_e2e.py
```
