package org.opentripplanner.ext.ridehailing.service.oauth;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import java.net.URI;
import java.net.http.HttpRequest;

/**
 * Implementation of an OAuth service that sends its parameters as a form url-endcoded
 * POST request.
 */
public class UrlEncodedOAuthService extends CachingOAuthService {

  private final ClientCredentialsRequest authRequest;
  private final URI uri;

  public UrlEncodedOAuthService(String clientSecret, String clientId, String scope, URI uri) {
    this.authRequest = new ClientCredentialsRequest(clientId, clientSecret, scope);
    this.uri = uri;
  }

  @Override
  protected HttpRequest oauthTokenRequest() {
    return HttpRequest.newBuilder(uri)
      .POST(HttpRequest.BodyPublishers.ofString(authRequest.toRequestParamString()))
      .header(CONTENT_TYPE, "application/x-www-form-urlencoded")
      .build();
  }
}
