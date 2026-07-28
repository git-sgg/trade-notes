-- 交易备注平台 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS trade_notes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE trade_notes;

-- 自选股表
CREATE TABLE IF NOT EXISTS stock_watch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL DEFAULT 'default',
    symbol VARCHAR(20) NOT NULL COMMENT '股票代码',
    name VARCHAR(100) COMMENT '股票名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_symbol (symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 交易记录表
CREATE TABLE IF NOT EXISTS trade_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_id BIGINT NOT NULL,
    trade_type VARCHAR(10) NOT NULL COMMENT 'BUY / SELL',
    price DECIMAL(10, 2) NOT NULL COMMENT '成交价格',
    quantity INT NOT NULL DEFAULT 100 COMMENT '成交股数',
    trade_date DATE NOT NULL COMMENT '交易日期',
    reason TEXT COMMENT '交易理由',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stock_id (stock_id),
    FOREIGN KEY (stock_id) REFERENCES stock_watch(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 七鲜超市门店表
CREATE TABLE IF NOT EXISTS seven_fresh_stores (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '门店名称',
    address VARCHAR(255) COMMENT '门店地址',
    start_time_online VARCHAR(10) COMMENT '线上营业开始',
    end_time_online VARCHAR(10) COMMENT '线上营业结束',
    start_time_offline VARCHAR(10) COMMENT '线下营业开始',
    end_time_offline VARCHAR(10) COMMENT '线下营业结束',
    longitude DECIMAL(10, 7) COMMENT '经度',
    latitude DECIMAL(10, 7) COMMENT '纬度',
    status INT COMMENT '状态',
    created_time DATETIME,
    INDEX idx_longitude (longitude),
    INDEX idx_latitude (latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
