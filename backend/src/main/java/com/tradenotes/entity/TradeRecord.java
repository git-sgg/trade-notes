package com.tradenotes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("trade_record")
public class TradeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stockId;

    private String tradeType; // BUY / SELL

    private BigDecimal price;

    private Integer quantity;   // 成交股数，默认100

    private LocalDate tradeDate;

    private String reason;

    private LocalDateTime createdAt;
}
