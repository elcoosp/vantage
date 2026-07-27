package com.vantage.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * InventoryUpdateRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-27T17:17:38.378056+02:00[Europe/Paris]", comments = "Generator version: 7.5.0")
public class InventoryUpdateRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private Integer quantity;

  public InventoryUpdateRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InventoryUpdateRequest(Integer quantity) {
    this.quantity = quantity;
  }

  public InventoryUpdateRequest quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Get quantity
   * @return quantity
  */
  @NotNull 
  @Schema(name = "quantity", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InventoryUpdateRequest inventoryUpdateRequest = (InventoryUpdateRequest) o;
    return Objects.equals(this.quantity, inventoryUpdateRequest.quantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(quantity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InventoryUpdateRequest {\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
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

