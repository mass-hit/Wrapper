package cn.kaguyaever.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class Item implements Serializable {
    private Integer id;
    private String itemName;
    private Double price;
    private Boolean delete;
}
