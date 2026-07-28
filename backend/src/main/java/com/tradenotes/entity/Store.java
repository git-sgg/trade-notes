package com.tradenotes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seven_fresh_stores")
public class Store {
    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String address;
    private String startTimeOnline;
    private String endTimeOnline;
    private String startTimeOffline;
    private String endTimeOffline;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;
    private LocalDateTime createdTime;
}
