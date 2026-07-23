package com.tradenotes.entity;

import lombok.Data;

import java.util.List;

/**
 * K线数据点，供前端ECharts使用
 */
@Data
public class KLinePoint {
    private String date;    // yyyy-MM-dd
    private double close;   // 收盘价
    private double open;   // 开盘价
    private double high;   // 最高价
    private double low;    // 最低价

    public KLinePoint(String date, double close, double open, double high, double low) {
        this.date = date;
        this.close = close;
        this.open = open;
        this.high = high;
        this.low = low;
    }
}
