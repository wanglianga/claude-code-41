# 城市外卖骑手装备领用与安全事故申报服务

基于 **Java 17 + Spring Boot 3 + PostgreSQL 16** 的站点级骑手装备与安全事故管理系统，覆盖装备登记发放、更换评估、事故申报核查、保险理赔、装备补发、骑手考核、培训记录与复盘归因分析的完整闭环。

## 原始需求

> 开发城市外卖骑手装备领用与安全事故申报服务，可采用 Java、Spring Boot 和 PostgreSQL。站点为骑手登记头盔、反光衣、雨衣、保温箱、电动车支架和充电器等装备，记录发放时间、尺码、押金、使用状态和更换周期。骑手申请更换时，服务根据磨损照片、天气、历史领用和站点库存判断是否免费更换、押金扣减或维修。发生交通事故、摔倒、装备损坏或餐品污染时，骑手提交事故时间、地点、订单、装备状态、伤情、交警记录和照片。站长需要核查是否在配送中、是否佩戴装备、是否违规骑行和是否需要保险材料。服务把事故、装备、订单、保险、补发和骑手考核串在同一事件里。若雨季装备集中损坏、头盔过期、保温箱影响食品安全或事故材料缺失，站点可以调整采购、培训和审核规则。站点还要按骑手、线路和天气查看事故与装备消耗，判断某类装备是否需要升级，或某些高风险区域是否要加强培训。服务还要把装备领用与事故安全关联起来，站点复盘时能判断事故是否与装备老化、未佩戴、配送压力或培训缺失有关，并调整采购和考核。

## 功能总览

| 模块 | 说明 |
| --- | --- |
| 装备管理 | 6 类装备（头盔/反光衣/雨衣/保温箱/电动车支架/充电器）类型、押金、更换周期、尺码、站点库存 |
| 装备发放 | 站长发放装备：扣减库存、记录发放时间/尺码/押金/状态，自动计算应更换日期 |
| 更换申请 | 骑手提交磨损照片（真实文件上传）+天气+原因，规则引擎按**使用时长 vs 更换周期、雨雪天加速损耗、近 6 个月更换频率、站点库存**自动评估：免费更换 / 押金扣减 / 维修 / 驳回，站长可覆盖 |
| 事故申报 | 骑手提交事故类型（交通事故/摔倒/装备损坏/餐品污染）、时间、地点、线路、订单号、装备状态、伤情、交警记录、现场照片（文件上传）、天气 |
| 照片附件 | 真实文件上传（JPG/PNG/GIF/WEBP，单张 ≤5MB，魔数校验防伪装），上传后回显缩略图，提交后事故/更换详情与站长核查页均可查看原图 |
| 事故核查 | 站长核查是否配送中、是否佩戴装备、是否违规、是否需要保险材料；核查通过自动**联动生成**保险理赔单、装备补发单、考核扣分 |
| 事件全景 | 事故 + 核查 + 保险 + 补发 + 考核在同一事件视图串联 |
| 风险告警 | 雨季装备集中损坏、头盔过期、保温箱食品安全、保险材料缺失四类自动告警 |
| 数据分析 | 事故按骑手/线路/天气分布，装备消耗统计（判断是否需升级装备） |
| 复盘归因 | 自动判断事故是否与装备老化、未佩戴、配送压力（90 天多起事故）、培训缺失（12 个月无安全培训）相关，并给出采购/考核/培训调整建议 |
| 规则调整 | 站点发布采购、培训、审核规则调整记录 |

## 快速开始（Docker 一键部署）

前置条件：Docker 与 Docker Compose。

```bash
cp .env.example .env     # 按需修改端口与数据库密码
docker compose up -d --build
```

- 应用启动后自动建表并初始化演示数据（仅首次）。
- 验证健康状态：`docker compose ps`（app 服务 healthy），或访问 `http://localhost:<CC_PUBLISH_PORT>/api/health`。
- 打开浏览器访问 `http://localhost:<CC_PUBLISH_PORT>/` 进入登录页。
- 取实际映射端口：`docker compose port app 8080`。
- 停止并释放资源：`docker compose down`（加 `-v` 同时清空数据）。

> 说明：仅应用服务端口发布到宿主；PostgreSQL 仅在 compose 内部网络中通过服务名 `db` 访问，不对外暴露。

## 测试账号（演示数据）

| 角色 | 用户名 | 密码 | 权限说明 |
| --- | --- | --- | --- |
| 站长 | `manager1` | `manager123` | 城东配送站：库存/发放、更换审核、事故核查、理赔、补发、考核、培训、分析、规则 |
| 站长 | `manager2` | `manager123` | 城西配送站：同上 |
| 骑手 | `rider01` | `rider123` | 张伟（城东站）：我的装备、申请更换、事故申报、考核/培训查看 |
| 骑手 | `rider02` | `rider123` | 李强（城东站）：同上 |
| 骑手 | `rider03` | `rider123` | 王芳（城西站）：同上 |
| 骑手 | `rider04` | `rider123` | 赵磊（城西站）：同上 |
| 管理员 | `admin` | `admin123` | 跨站点查看与分析（站长端可切换站点） |

演示数据包含：2 个站点、6 类装备及库存、20+ 条历史领用（部分已超期）、4 条更换申请（2 待审 2 已处理）、7 起事故（含已核查联动保险/补发/考核）、培训记录与站点规则。

## 建议演示路径

1. **骑手 rider01 登录** →「我的装备」看到超期头盔/雨衣 → 对雨衣「申请更换」（选雨天）→「更换申请」查看系统自动评估结论。
2. **站长 manager1 登录** →「总览」查看风险告警（雨季集中损坏、头盔过期、材料缺失）→「更换审核」按系统建议选择免费更换/扣押金/维修 →「事故核查」对待核查事故做核查（勾选需要保险、装备损坏）→ 自动在「保险理赔」「装备补发」「骑手考核」看到联动记录。
3. **站长端「数据分析」** → 查看按骑手/线路/天气的事故分布、装备消耗与「事故复盘归因」（装备老化/未佩戴/配送压力/培训缺失）→ 在「规则调整」发布采购或培训规则。

## 验证方式

本服务的验证标准为 **宿主机 `docker compose up -d` 一键启动成功 + 关键业务流可走通**：

1. `docker compose up -d --build` 后 `docker compose ps` 两个服务均为 healthy/running；
2. `curl http://localhost:<CC_PUBLISH_PORT>/api/health` 返回 `{"status":"UP"}`；
3. 浏览器走通上述「建议演示路径」，或参考下方 API 一览用 curl 验证。

## API 一览（节选）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录（JSON: username/password，会话 Cookie） |
| GET | `/api/auth/me` | 当前用户 |
| POST | `/api/attachments` | 上传照片（multipart 字段名 `file`，JPG/PNG/GIF/WEBP ≤5MB，返回附件 ID 与 URL） |
| GET | `/api/attachments/{id}` | 读取照片内容（登录用户可访问，用于回显与复核） |
| GET | `/api/rider/equipment` | 我的装备 |
| POST | `/api/rider/replacements` | 提交更换申请（自动评估，body 可带 `photoIds: [附件ID]`） |
| POST | `/api/rider/accidents` | 提交事故申报（body 可带 `photoIds: [附件ID]`） |
| GET | `/api/manager/stock` / POST `/api/manager/issue` | 站点库存 / 发放装备 |
| POST | `/api/manager/replacements/{id}/process` | 更换审核（APPROVE_FREE/APPROVE_DEPOSIT/REPAIR/REJECT） |
| POST | `/api/manager/accidents/{id}/review` | 事故核查（联动保险/补发/考核） |
| GET | `/api/manager/claims` / PUT `/api/manager/claims/{id}` | 理赔单管理 |
| POST | `/api/manager/reissues/{id}/process` | 补发单发放/取消 |
| GET | `/api/analytics/dashboard` `/alerts` `/retrospective` `/accidents/by-rider` `/accidents/by-route` `/accidents/by-weather` `/equipment/consumption` | 统计分析 |
| GET/POST/DELETE | `/api/manager/policies` | 采购/培训/审核规则调整 |

## 技术栈与结构

- 后端：Spring Boot 3.3（Web / Data JPA / Security / Validation），会话认证 + 角色鉴权（RIDER / STATION_MANAGER / ADMIN）
- 数据库：PostgreSQL 16（JPA 自动建表，首启初始化演示数据）
- 前端：静态 HTML + 原生 JS（登录页 / 骑手端 / 站长端），由 Spring Boot 直接托管
- 部署：多阶段 Dockerfile（非 root 运行 + HEALTHCHECK）+ docker-compose（app + db，仅应用端口发布）

```
├── Dockerfile                 # 多阶段构建（maven 构建 + jre 运行，非 root，健康检查）
├── docker-compose.yml         # app + postgres（db 不发布端口）
├── .env.example               # 端口与数据库配置样例
├── pom.xml
└── src/main/
    ├── java/com/example/ridersafety/
    │   ├── config/            # 安全配置、全局异常、演示数据初始化
    │   ├── controller/        # REST 控制器（认证/装备/更换/事故/考核/培训/规则/分析）
    │   ├── model/             # JPA 实体与枚举
    │   ├── repository/        # Spring Data JPA 仓库
    │   ├── service/           # 业务逻辑（更换规则引擎、事故联动、统计归因）
    │   └── util/              # 请求参数辅助
    └── resources/
        ├── application.yml
        └── static/            # 前端页面（login / rider / manager）
```
