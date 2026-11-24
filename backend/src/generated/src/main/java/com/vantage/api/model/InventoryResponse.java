package com.vantage.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * InventoryResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-25T00:16:11.974644+01:00[Europe/Paris]", comments = "Generator version: 7.26.0")
public class InventoryResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  private @Nullable UUID productId;

  private @Nullable Integer quantity;

  private @Nullable Integer version;

  public InventoryResponse productId(@Nullable UUID productId) {
    this.productId = productId;
    return this;
  }

  /**
   * Get productId
   * @return productId
   */
  @Valid 
  @Schema(name = "productId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("productId")
  public @Nullable UUID getProductId() {
    return productId;
  }

  @JsonProperty("productId")
  public void setProductId(@Nullable UUID productId) {
    this.productId = productId;
  }

  public InventoryResponse quantity(@Nullable Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Get quantity
   * @return quantity
   */
  
  @Schema(name = "quantity", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("quantity")
  public @Nullable Integer getQuantity() {
    return quantity;
  }

  @JsonProperty("quantity")
  public void setQuantity(@Nullable Integer quantity) {
    this.quantity = quantity;
  }

  public InventoryResponse version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * The new version number to be used in future If-Match headers.
   * @return version
   */
  
  @Schema(name = "version", description = "The new version number to be used in future If-Match headers.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("version")
  public @Nullable Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(@Nullable Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InventoryResponse inventoryResponse = (InventoryResponse) o;
    return Objects.equals(this.productId, inventoryResponse.productId) &&
        Objects.equals(this.quantity, inventoryResponse.quantity) &&
        Objects.equals(this.version, inventoryResponse.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId, quantity, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InventoryResponse {\n");
    sb.append("    productId: ").append(toIndentedString(productId)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

