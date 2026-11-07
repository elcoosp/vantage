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
 * WebhookConfigResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-07T10:31:28.165799286Z[Etc/UTC]", comments = "Generator version: 7.5.0")
public class WebhookConfigResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  private String webhookUrl;

  private String secret;

  public WebhookConfigResponse webhookUrl(String webhookUrl) {
    this.webhookUrl = webhookUrl;
    return this;
  }

  /**
   * Registered webhook URL, empty string when unset.
   * @return webhookUrl
  */
  
  @Schema(name = "webhookUrl", description = "Registered webhook URL, empty string when unset.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("webhookUrl")
  public String getWebhookUrl() {
    return webhookUrl;
  }

  public void setWebhookUrl(String webhookUrl) {
    this.webhookUrl = webhookUrl;
  }

  public WebhookConfigResponse secret(String secret) {
    this.secret = secret;
    return this;
  }

  /**
   * Masked signing secret (e.g. abcd…wxyz), null when unset.
   * @return secret
  */
  
  @Schema(name = "secret", description = "Masked signing secret (e.g. abcd…wxyz), null when unset.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secret")
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebhookConfigResponse webhookConfigResponse = (WebhookConfigResponse) o;
    return Objects.equals(this.webhookUrl, webhookConfigResponse.webhookUrl) &&
        Objects.equals(this.secret, webhookConfigResponse.secret);
  }

  @Override
  public int hashCode() {
    return Objects.hash(webhookUrl, secret);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebhookConfigResponse {\n");
    sb.append("    webhookUrl: ").append(toIndentedString(webhookUrl)).append("\n");
    sb.append("    secret: ").append(toIndentedString(secret)).append("\n");
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

