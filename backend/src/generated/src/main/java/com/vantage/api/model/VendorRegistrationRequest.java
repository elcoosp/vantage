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
 * VendorRegistrationRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-27T16:25:39.140575+02:00[Europe/Paris]", comments = "Generator version: 7.5.0")
public class VendorRegistrationRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String email;

  private String password;

  private String storeName;

  private String tenantSlug;

  public VendorRegistrationRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public VendorRegistrationRequest(String email, String password, String storeName, String tenantSlug) {
    this.email = email;
    this.password = password;
    this.storeName = storeName;
    this.tenantSlug = tenantSlug;
  }

  public VendorRegistrationRequest email(String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
  */
  @NotNull @jakarta.validation.constraints.Email 
  @Schema(name = "email", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public VendorRegistrationRequest password(String password) {
    this.password = password;
    return this;
  }

  /**
   * Get password
   * @return password
  */
  @NotNull 
  @Schema(name = "password", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public VendorRegistrationRequest storeName(String storeName) {
    this.storeName = storeName;
    return this;
  }

  /**
   * Get storeName
   * @return storeName
  */
  @NotNull 
  @Schema(name = "storeName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("storeName")
  public String getStoreName() {
    return storeName;
  }

  public void setStoreName(String storeName) {
    this.storeName = storeName;
  }

  public VendorRegistrationRequest tenantSlug(String tenantSlug) {
    this.tenantSlug = tenantSlug;
    return this;
  }

  /**
   * Get tenantSlug
   * @return tenantSlug
  */
  @NotNull 
  @Schema(name = "tenantSlug", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("tenantSlug")
  public String getTenantSlug() {
    return tenantSlug;
  }

  public void setTenantSlug(String tenantSlug) {
    this.tenantSlug = tenantSlug;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VendorRegistrationRequest vendorRegistrationRequest = (VendorRegistrationRequest) o;
    return Objects.equals(this.email, vendorRegistrationRequest.email) &&
        Objects.equals(this.password, vendorRegistrationRequest.password) &&
        Objects.equals(this.storeName, vendorRegistrationRequest.storeName) &&
        Objects.equals(this.tenantSlug, vendorRegistrationRequest.tenantSlug);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, password, storeName, tenantSlug);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VendorRegistrationRequest {\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    storeName: ").append(toIndentedString(storeName)).append("\n");
    sb.append("    tenantSlug: ").append(toIndentedString(tenantSlug)).append("\n");
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

