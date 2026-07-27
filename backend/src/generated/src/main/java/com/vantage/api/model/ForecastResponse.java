package com.vantage.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vantage.api.model.ForecastResponseForecastInner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * ForecastResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-27T16:25:39.140575+02:00[Europe/Paris]", comments = "Generator version: 7.5.0")
public class ForecastResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID productId;

  @Valid
  private List<@Valid ForecastResponseForecastInner> forecast = new ArrayList<>();

  public ForecastResponse productId(UUID productId) {
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
  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public ForecastResponse forecast(List<@Valid ForecastResponseForecastInner> forecast) {
    this.forecast = forecast;
    return this;
  }

  public ForecastResponse addForecastItem(ForecastResponseForecastInner forecastItem) {
    if (this.forecast == null) {
      this.forecast = new ArrayList<>();
    }
    this.forecast.add(forecastItem);
    return this;
  }

  /**
   * Get forecast
   * @return forecast
  */
  @Valid 
  @Schema(name = "forecast", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("forecast")
  public List<@Valid ForecastResponseForecastInner> getForecast() {
    return forecast;
  }

  public void setForecast(List<@Valid ForecastResponseForecastInner> forecast) {
    this.forecast = forecast;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForecastResponse forecastResponse = (ForecastResponse) o;
    return Objects.equals(this.productId, forecastResponse.productId) &&
        Objects.equals(this.forecast, forecastResponse.forecast);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId, forecast);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForecastResponse {\n");
    sb.append("    productId: ").append(toIndentedString(productId)).append("\n");
    sb.append("    forecast: ").append(toIndentedString(forecast)).append("\n");
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

