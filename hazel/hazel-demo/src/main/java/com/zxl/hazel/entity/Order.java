package com.zxl.hazel.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private String orderNo;
    private String productName;
    private BigDecimal amount;
    private String status;
    private Date createdAt;


    public Order(String orderNo, String productName, BigDecimal amount, String status) {
        this.orderNo = orderNo;
        this.productName = productName;
        this.amount = amount;
        this.status = status;
    }

}