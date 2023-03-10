package org.opentripplanner.ext.carhailing.service.uber;

/**
 * Data structure for requesting an Uber access token.
 */
public class UberAuthenticationRequestBody {

  public final String clientId;
  public final String clientSecret;
  public final String grantType;
  public final String scope;

  public UberAuthenticationRequestBody(String clientId, String clientSecret) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    // Defaults needed for price/time estimates.
    this.grantType = "client_credentials";
    this.scope = "ride_request.estimate";
  }

  /**
   * Converts this object to application/x-www-form-urlencoded format ("name1=value1&name2=value2").
   * (There should be no need to url-encode as there are no special characters in the values passed.)
   */
  public String toRequestParamString() {
    return String.format(
      "client_id=%s&client_secret=%s&grant_type=%s&scope=%s",
      clientId,
      clientSecret,
      grantType,
      scope
    );
  }
}
