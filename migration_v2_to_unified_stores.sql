-- =============================================
-- 合并三品牌门店表 → 统一 stores 表
-- =============================================

-- Step 1: 创建统一表
CREATE TABLE IF NOT EXISTS stores (
    id VARCHAR(64) PRIMARY KEY COMMENT '品牌前缀+原ID，如 seven_xxx, hema001, sam_1',
    brand ENUM('seven', 'hema', 'sam') NOT NULL COMMENT '品牌标识',
    name VARCHAR(100) NOT NULL COMMENT '门店名称',
    address VARCHAR(255) COMMENT '门店地址',
    start_time_online VARCHAR(10) COMMENT '线上营业开始',
    end_time_online VARCHAR(10) COMMENT '线上营业结束',
    start_time_offline VARCHAR(10) COMMENT '线下营业开始',
    end_time_offline VARCHAR(10) COMMENT '线下营业结束',
    longitude DECIMAL(10, 7) COMMENT '经度',
    latitude DECIMAL(10, 7) COMMENT '纬度',
    status INT COMMENT '状态（0=营业，1=筹建）',
    created_time DATETIME,
    INDEX idx_brand (brand),
    INDEX idx_longitude (longitude),
    INDEX idx_latitude (latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: 从 seven_fresh_stores 迁移（ID 加 seven_ 前缀）
INSERT INTO stores (id, brand, name, address, start_time_online, end_time_online, start_time_offline, end_time_offline, longitude, latitude, status, created_time)
SELECT CONCAT('seven_', id), 'seven', name, address, start_time_online, end_time_online, start_time_offline, end_time_offline, longitude, latitude, status, created_time
FROM seven_fresh_stores
ON DUPLICATE KEY UPDATE brand=VALUES(brand), name=VALUES(name), address=VALUES(address);

-- Step 3: 从 hema_stores 迁移（ID 加 hema_ 前缀）
INSERT INTO stores (id, brand, name, address, start_time_online, end_time_online, start_time_offline, end_time_offline, longitude, latitude, status, created_time)
SELECT CONCAT('hema_', id), 'hema', name, address, start_time_online, end_time_online, start_time_offline, end_time_offline, longitude, latitude, status, created_time
FROM hema_stores
ON DUPLICATE KEY UPDATE brand=VALUES(brand), name=VALUES(name), address=VALUES(address);

-- Step 4: 从 sam_stores 迁移（ID 加 sam_ 前缀）
INSERT INTO stores (id, brand, name, address, start_time_online, end_time_online, start_time_offline, end_time_offline, longitude, latitude, status, created_time)
SELECT CONCAT('sam_', id), 'sam', name, address, start_time_online, end_time_online, start_time_offline, end_time_offline, longitude, latitude, status, created_time
FROM sam_stores
ON DUPLICATE KEY UPDATE brand=VALUES(brand), name=VALUES(name), address=VALUES(address);

-- Step 5: 验证
SELECT brand, COUNT(*) as cnt FROM stores GROUP BY brand;
SELECT * FROM stores;
