# 交易备注平台

记录自选股交易笔记，可视化 K 线与买卖点。
<img width="2828" height="1344" alt="image" src="https://github.com/user-attachments/assets/255abb13-ea44-4b6e-9a20-45de8e33d0a9" />

---

## 快速启动

### 方式一：Docker 一键部署（推荐 NAS）

```bash
cd trade-notes
docker-compose up -d
```

访问：http://你的NAS IP/

---

### 方式二：本地开发调试

#### 前置条件
- JDK 17+
- Maven 3.9+
- Node.js 20+
- Docker Desktop（提供 MySQL）

#### 1. 启动 MySQL（Docker）

```bash
docker run -d --name trade-notes-mysql \
  -e MYSQL_ROOT_PASSWORD=trade123 \
  -e MYSQL_DATABASE=trade_notes \
  -p 3307:3306 \
  mysql:8
```

#### 2. 初始化数据库

```bash
docker exec -i trade-notes-mysql mysql -uroot -ptrade123 trade_notes < init.sql
```

#### 3. 启动后端（8888 端口）

```bash
cd backend

# 使用环境变量传入 Alpha Vantage Key
# 方式一：export（推荐）
export STOCK_API_KEY=你的Key
export DB_HOST=localhost
export DB_PORT=3307
mvn spring-boot:run

# 方式二：命令行参数
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--stock.api-key=你的Key --spring.datasource.url=jdbc:mysql://localhost:3307/trade_notes?..."

# 方式三：JAR 直接运行
java -jar target/trade-notes-backend-1.0.0.jar \
  --spring.datasource.url="jdbc:mysql://localhost:3307/trade_notes?..." \
  --stock.api-key=你的Key
```

#### 4. 启动前端（5173 端口）

```bash
cd frontend
npm install
npm run dev
```

访问：http://localhost:5173/

---

## 功能说明

### 添加股票
在顶部输入框输入股票代码：
- **美股**：如 `AAPL`、`TSLA`、`MSFT`
- **A股**：如 `600519`（茅台）、`000001`（平安）

### K 线图
- 默认显示最近 180 天走势
- **左右拖动**：缩放时间范围
- **滚轮**：放大/缩小
- 当 Yahoo Finance 数据源不可用时，自动使用模拟数据展示

### 交易记录
- 点击 **买入 / 卖出** 按钮，填写价格、日期、理由
- 买卖点标注在 K 线图上（🔴买入 / 🟢卖出）
- 鼠标悬停显示交易详情（价格、日期、理由）
- 可删除单条记录

### 数据源
- **主力**：Yahoo Finance（免费，无需 API Key）
- **降级**：模拟数据（当 Yahoo 不可用时）
- ⚠️ Alpha Vantage 免费版每天 **25 次请求**，每次返回约 100 条日线数据。
  - 已内置 10 分钟内存缓存，同一股票 10 分钟内不重复请求
  - 加载 5 只股票仅消耗 5 次（远低于限额）
  - 如需更高频率，申请 [Twelve Data](https://twelvedata.com/pricing) 免费版作为备用

---

## 项目结构

```
trade-notes/
├── docker-compose.yml    # Docker 部署配置
├── init.sql              # 数据库初始化脚本
├── backend/              # Spring Boot 后端
│   ├── src/main/java/com/tradenotes/
│   │   ├── controller/   # REST API
│   │   ├── service/      # 业务逻辑
│   │   ├── mapper/       # MyBatis-Plus Mapper
│   │   └── entity/        # 数据实体
│   └── Dockerfile
└── frontend/             # Vue3 前端
    ├── src/
    │   ├── App.vue       # 主组件（ECharts + 弹窗）
    │   └── api/index.js  # API 调用
    ├── nginx.conf        # Nginx 配置（含 API 代理）
    └── Dockerfile
```

## API 列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/stocks` | 获取自选股列表 |
| POST | `/api/stocks` | 添加股票 {symbol} |
| DELETE | `/api/stocks/{id}` | 删除自选股 |
| GET | `/api/stocks/{id}/prices?days=90` | 获取 K 线数据 |
| GET | `/api/stocks/{id}/records` | 获取交易记录 |
| POST | `/api/stocks/{id}/records` | 添加交易记录 |
| DELETE | `/api/stocks/records/{id}` | 删除交易记录 |

---

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + ECharts 5 + Axios |
| 后端 | Spring Boot 3.2 + MyBatis-Plus |
| 数据库 | MySQL 8 |
| 部署 | Docker + Nginx |
