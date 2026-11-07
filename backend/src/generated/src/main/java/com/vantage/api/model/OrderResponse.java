package com.vantage.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * OrderResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-07T10:31:28.165799286Z[Etc/UTC]", comments = "Generator version: 7.5.0")
public class OrderResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID orderId;

  private UUID productId;

  private Integer quantity;

  /**
   * CREATED orders are processed asynchronously by the RabbitMQ saga (inventory reservation + payment) and typically reach PAID within seconds. SHIPPED/DELIVERED are set by the fulfillment endpoints. 
   */
  public enum StatusEnum {
    CREATED("CREATED"),
    
    CONFIRMED("CONFIRMED"),
    
    PAID("PAID"),
    
    SHIPPED("SHIPPED"),
    
    DELIVERED("DELIVERED"),
    
    CANCELLED("CANCELLED");

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private StatusEnum status;

  public OrderResponse orderId(UUID orderId) {
    this.orderId = orderId;
    return this;
  }

  /**
   * Get orderId
   * @return orderId
  */
  @Valid 
  @Schema(name = "orderId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("orderId")
  public UUID getOrderId() {
    return orderId;
  }

  public void setOrderId(UUID orderId) {
    this.orderId = orderId;
  }

  public OrderResponse productId(UUID productId) {
    this.productId = productId;
    return this;
  }

  /**
   * Present on responses from the non-generated controller (202 flow).
   * @return productId
  */
  @Valid 
  @Schema(name = "productId", description = "Present on responses from the non-generated controller (202 flow).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("productId")
  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public OrderResponse quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Present on responses from the non-generated controller (202 flow).
   * @return quantity
  */
  
  @Schema(name = "quantity", description = "Present on responses from the non-generated controller (202 flow).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public OrderResponse status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * CREATED orders are processed asynchronously by the RabbitMQ saga (inventory reservation + payment) and typically reach PAID within seconds. SHIPPED/DELIVERED are set by the fulfillment endpoints. 
   * @return status
  */
  
  @Schema(name = "status", description = "CREATED orders are processed asynchronously by the RabbitMQ saga (inventory reservation + payment) and typically reach PAID within seconds. SHIPPED/DELIVERED are set by the fulfillment endpoints. ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderResponse orderResponse = (OrderResponse) o;
    return Objects.equals(this.orderId, orderResponse.orderId) &&
        Objects.equals(this.productId, orderResponse.productId) &&
        Objects.equals(this.quantity, orderResponse.quantity) &&
        Objects.equals(this.status, orderResponse.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(orderId, productId, quantity, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OrderResponse {\n");
    sb.append("    orderId: ").append(toIndentedString(orderId)).append("\n");
    sb.append("    productId: ").append(toIndentedString(productId)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

