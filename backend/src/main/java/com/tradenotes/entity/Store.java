package com.tradenotes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stores")
public class Store {
    @TableId(type = IdType.INPUT)
    private String id;

    private String brand;          // seven / hema / sam
    private String name;
    private String address;
    private String startTimeOnline;
    private String endTimeOnline;
    private String startTimeOffline;
    private String endTimeOffline;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;        // 0=营业 1=筹建
    private LocalDateTime createdTime;
}
