package com.tradenotes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_watch")
public class StockWatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String symbol;

    private String name;

    private LocalDateTime createdAt;
}
