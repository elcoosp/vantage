package com.vantage.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-27T17:18:07.183202+02:00[Europe/Paris]", comments = "Generator version: 7.5.0")
public class ForecastResponseForecastInner implements Serializable {

  private static final long serialVersionUID = 1L;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  private Integer predictedQuantity;

  private Integer lowerBound;

  private Integer upperBound;

  public ForecastResponseForecastInner date(LocalDate date) {
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
  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public ForecastResponseForecastInner predictedQuantity(Integer predictedQuantity) {
    this.predictedQuantity = predictedQuantity;
    return this;
  }

  /**
   * Get predictedQuantity
   * @return predictedQuantity
  */
  
  @Schema(name = "predictedQuantity", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("predictedQuantity")
  public Integer getPredictedQuantity() {
    return predictedQuantity;
  }

  public void setPredictedQuantity(Integer predictedQuantity) {
    this.predictedQuantity = predictedQuantity;
  }

  public ForecastResponseForecastInner lowerBound(Integer lowerBound) {
    this.lowerBound = lowerBound;
    return this;
  }

  /**
   * Get lowerBound
   * @return lowerBound
  */
  
  @Schema(name = "lowerBound", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lowerBound")
  public Integer getLowerBound() {
    return lowerBound;
  }

  public void setLowerBound(Integer lowerBound) {
    this.lowerBound = lowerBound;
  }

  public ForecastResponseForecastInner upperBound(Integer upperBound) {
    this.upperBound = upperBound;
    return this;
  }

  /**
   * Get upperBound
   * @return upperBound
  */
  
  @Schema(name = "upperBound", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("upperBound")
  public Integer getUpperBound() {
    return upperBound;
  }

  public void setUpperBound(Integer upperBound) {
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
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

