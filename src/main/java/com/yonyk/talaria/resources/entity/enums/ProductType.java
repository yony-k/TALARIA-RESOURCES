package com.yonyk.talaria.resources.entity.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductType {
  @JsonProperty("매입")
  PURCHASE("PURCHASE"),

  @JsonProperty("매출")
  SALE("SALE");

  private final String productType;
}
