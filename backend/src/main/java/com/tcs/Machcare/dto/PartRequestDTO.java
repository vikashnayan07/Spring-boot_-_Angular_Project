package com.tcs.Machcare.dto;

public class PartRequestDTO {
    private Long partId;
    private Integer quantity;

    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}