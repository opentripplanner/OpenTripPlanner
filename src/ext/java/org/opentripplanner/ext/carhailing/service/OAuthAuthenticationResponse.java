package org.opentripplanner.ext.carhailing.service;

/**
 * Data structure for a response to an OAuth access token request.
 * All fields except error are populated if the request is successful.
 * If the request failed, only the error field is populated.
 */
public class OAuthAuthenticationResponse {

  public String access_token;
  public String error;
  public int expires_in;
  public String scope;
  public String token_type;
}
