# 交易备注平台

记录自选股交易笔记，可视化 K 线与买卖点。
<img width="2828" height="1344" alt="image" src="https://github.com/user-attachments/assets/255abb13-ea44-4b6e-9a20-45de8e33d0a9" />

---

## 功能说明

### 自选股管理
- 输入股票代码添加自选（自动解析中文名）
- 支持 A 股（6位代码）和美股
- 删除股票

### K 线图
- A 股使用腾讯历史 K 线（近 90 天，含今日）
- 点击买入/卖出按钮记录交易
- 买卖点标注在 K 线图上（🔴买入 / 🟢卖出）
- 鼠标悬停显示交易详情

### 交易记录
- 填写价格、日期、理由（可选）
- 记录成交股数（默认 100 股）
- 支持编辑和删除
- 股票卡片显示当前盈亏

---

## 快速部署

### 方式一：NAS Docker（推荐）

**前置条件：** NAS 上已安装 Docker，可通过 GUI 管理容器。

#### 1. 导入镜像

从 GitHub Releases 或本地构建获得 `trade-notes-backend.tar`，导入到 NAS Docker。

#### 2. 创建 MySQL 容器

| 配置项 | 值 |
|--------|---|
| 容器名称 | `mysql` |
| 镜像 | `mysql:8` |
| 端口 | `3306:3306` |
| 环境变量 | `MYSQL_ROOT_PASSWORD=trade123`<br>`MYSQL_DATABASE=trade_notes` |
| 自动重启 | ✅ |

**初始化数据库**（SSH 或容器终端执行）：
```sql
docker exec -it mysql mysql -uroot -ptrade123 -e "
CREATE DATABASE IF NOT EXISTS trade_notes CHARACTER SET utf8mb4;
USE trade_notes;
CREATE TABLE IF NOT EXISTS stock_watch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL DEFAULT 'default',
    symbol VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS trade_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_id BIGINT NOT NULL,
    trade_type VARCHAR(10) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 100,
    trade_date DATE NOT NULL,
    reason TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
"
```

#### 3. 创建后端容器

| 配置项 | 值 |
|--------|---|
| 容器名称 | `trade-notes-backend` |
| 镜像 | `trade-notes-backend:latest` |
| 端口 | `8888:8888` |
| 环境变量 | `DB_HOST=mysql`<br>`DB_PORT=3306`<br>`DB_USER=root`<br>`DB_PASS=trade123` |
| 自动重启 | ✅ |

#### 4. 访问

```
http://NAS_IP:8888/
```

---

### 方式二：本地开发

#### 前置条件

- JDK 17+
- Maven 3.9+
- Node.js 20+
- Docker（提供 MySQL）

#### 1. 启动 MySQL

```bash
docker run -d --name trade-notes-mysql \
  -e MYSQL_ROOT_PASSWORD=trade123 \
  -e MYSQL_DATABASE=trade_notes \
  -p 3307:3306 \
  mysql:8
```

#### 2. 初始化数据库

```bash
docker exec -i trade-notes-mysql mysql -uroot -ptrade123 < init.sql
```

#### 3. 启动后端

```bash
cd backend
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://localhost:3307/trade_notes"
```

后端启动后监听 `8888` 端口。

#### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173/`（Vite 开发服务器会自动代理 `/api` 到后端 8888）

#### 5. 构建前端

```bash
npm run build
```

前端打包到 `backend/src/main/resources/static/`，重启后端即可生效。

---

## 项目结构

```
trade-notes/
├── backend/              # Spring Boot 后端
│   ├── src/main/java/com/tradenotes/
│   │   ├── controller/    # REST API
│   │   ├── service/       # 业务逻辑
│   │   ├── mapper/        # MyBatis-Plus Mapper
│   │   └── entity/       # 数据实体
│   ├── src/main/resources/
│   │   └── static/        # 前端打包输出（自动生成）
│   └── Dockerfile
├── frontend/             # Vue3 前端
│   ├── src/
│   │   └── App.vue       # 主组件
│   ├── vite.config.js    # 构建输出到 ../backend/src/main/resources/static
│   └── package.json
├── docker-compose.yml
└── init.sql
```

---

## API 列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/stocks` | 获取自选股列表 |
| POST | `/api/stocks` | 添加股票 {symbol} |
| DELETE | `/api/stocks/{id}` | 删除自选股 |
| GET | `/api/stocks/{id}/prices?days=90` | 获取 K 线数据 |
| GET | `/api/stocks/{id}/records` | 获取交易记录 |
| POST | `/api/stocks/{id}/records` | 添加交易记录 |
| PUT | `/api/stocks/records/{id}` | 编辑交易记录 |
| DELETE | `/api/stocks/records/{id}` | 删除交易记录 |

---

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + ECharts 5 + Axios + Vite |
| 后端 | Spring Boot 3.2 + MyBatis-Plus |
| 数据库 | MySQL 8 |
| K 线数据 | 腾讯历史 K 线 API（A股）/ Alpha Vantage（美股） |
| 部署 | Docker（前后端合并镜像） |
