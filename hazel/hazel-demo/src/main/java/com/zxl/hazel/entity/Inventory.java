package com.zxl.hazel.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    private Long id;
    private String productName;
    private Integer quantity;
    private Date updatedAt;
    


}