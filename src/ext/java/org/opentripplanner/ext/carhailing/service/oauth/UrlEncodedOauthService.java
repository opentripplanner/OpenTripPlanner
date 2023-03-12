package org.opentripplanner.ext.carhailing.service.oauth;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import java.net.URI;
import java.net.http.HttpRequest;
import org.opentripplanner.ext.carhailing.service.uber.UberAuthenticationRequestBody;

public class UrlEncodedOauthService extends OAuthService {

  private final String clientSecret;
  private final String clientId;

  private final URI uri;

  public UrlEncodedOauthService(String clientSecret, String clientId, URI uri) {
    this.clientSecret = clientSecret;
    this.clientId = clientId;
    this.uri = uri;
  }

  @Override
  protected HttpRequest oauthTokenRequest() {
    // set request body
    UberAuthenticationRequestBody authRequest = new UberAuthenticationRequestBody(
      clientId,
      clientSecret
    );

    return HttpRequest
      .newBuilder(uri)
      .POST(HttpRequest.BodyPublishers.ofString(authRequest.toRequestParamString()))
      .header(CONTENT_TYPE, "application/x-www-form-urlencoded")
      .build();
  }
}
