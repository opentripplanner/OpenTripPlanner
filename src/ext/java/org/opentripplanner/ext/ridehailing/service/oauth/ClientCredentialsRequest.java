package org.opentripplanner.ext.ridehailing.service.oauth;

/**
 * Data structure for requesting an Uber access token.
 */
public class ClientCredentialsRequest {

  public final String clientId;
  public final String clientSecret;
  public final String grantType = "client_credentials";
  public final String scope;

  public ClientCredentialsRequest(String clientId, String clientSecret, String scope) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.scope = scope;
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
