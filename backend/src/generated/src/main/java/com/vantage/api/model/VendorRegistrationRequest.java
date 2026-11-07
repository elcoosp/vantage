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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-07T10:31:28.165799286Z[Etc/UTC]", comments = "Generator version: 7.5.0")
public class VendorRegistrationRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String email;

  private String password;

  private String name;

  public VendorRegistrationRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public VendorRegistrationRequest(String email, String password, String name) {
    this.email = email;
    this.password = password;
    this.name = name;
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
  @NotNull @Size(min = 8) 
  @Schema(name = "password", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public VendorRegistrationRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Company / store name shown in the dashboard.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "Company / store name shown in the dashboard.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
        Objects.equals(this.name, vendorRegistrationRequest.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, password, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VendorRegistrationRequest {\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

