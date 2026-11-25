package com.vantage.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
 * ForecastResponseForecastInner
 */

@JsonTypeName("ForecastResponse_forecast_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-25T01:18:12.645984+01:00[Europe/Paris]", comments = "Generator version: 7.26.0")
public class ForecastResponseForecastInner implements Serializable {

  private static final long serialVersionUID = 1L;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate date;

  private @Nullable Integer predictedQuantity;

  private @Nullable Integer lowerBound;

  private @Nullable Integer upperBound;

  public ForecastResponseForecastInner date(@Nullable LocalDate date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
   * @return date
   */
  @Valid 
  @Schema(name = "date", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("date")
  public @Nullable LocalDate getDate() {
    return date;
  }

  @JsonProperty("date")
  public void setDate(@Nullable LocalDate date) {
    this.date = date;
  }

  public ForecastResponseForecastInner predictedQuantity(@Nullable Integer predictedQuantity) {
    this.predictedQuantity = predictedQuantity;
    return this;
  }

  /**
   * Get predictedQuantity
   * @return predictedQuantity
   */
  
  @Schema(name = "predictedQuantity", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("predictedQuantity")
  public @Nullable Integer getPredictedQuantity() {
    return predictedQuantity;
  }

  @JsonProperty("predictedQuantity")
  public void setPredictedQuantity(@Nullable Integer predictedQuantity) {
    this.predictedQuantity = predictedQuantity;
  }

  public ForecastResponseForecastInner lowerBound(@Nullable Integer lowerBound) {
    this.lowerBound = lowerBound;
    return this;
  }

  /**
   * Get lowerBound
   * @return lowerBound
   */
  
  @Schema(name = "lowerBound", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lowerBound")
  public @Nullable Integer getLowerBound() {
    return lowerBound;
  }

  @JsonProperty("lowerBound")
  public void setLowerBound(@Nullable Integer lowerBound) {
    this.lowerBound = lowerBound;
  }

  public ForecastResponseForecastInner upperBound(@Nullable Integer upperBound) {
    this.upperBound = upperBound;
    return this;
  }

  /**
   * Get upperBound
   * @return upperBound
   */
  
  @Schema(name = "upperBound", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("upperBound")
  public @Nullable Integer getUpperBound() {
    return upperBound;
  }

  @JsonProperty("upperBound")
  public void setUpperBound(@Nullable Integer upperBound) {
    this.upperBound = upperBound;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForecastResponseForecastInner forecastResponseForecastInner = (ForecastResponseForecastInner) o;
    return Objects.equals(this.date, forecastResponseForecastInner.date) &&
        Objects.equals(this.predictedQuantity, forecastResponseForecastInner.predictedQuantity) &&
        Objects.equals(this.lowerBound, forecastResponseForecastInner.lowerBound) &&
        Objects.equals(this.upperBound, forecastResponseForecastInner.upperBound);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, predictedQuantity, lowerBound, upperBound);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForecastResponseForecastInner {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    predictedQuantity: ").append(toIndentedString(predictedQuantity)).append("\n");
    sb.append("    lowerBound: ").append(toIndentedString(lowerBound)).append("\n");
    sb.append("    upperBound: ").append(toIndentedString(upperBound)).append("\n");
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

