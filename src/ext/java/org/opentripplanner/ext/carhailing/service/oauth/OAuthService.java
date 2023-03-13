package org.opentripplanner.ext.carhailing.service.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.opentripplanner.ext.carhailing.service.CarHailingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(CarHailingService.class);
  private CachedOAuthToken cachedToken = CachedOAuthToken.empty();

  /**
   * Obtains and caches an OAuth API access token.
   * @return A token holder with the token value (including null token values if the call was unsuccessful).
   */
  public String getToken() {
    if (cachedToken.isExpired()) {
      // prepare request to get token
      try {
        var request = oauthTokenRequest();
        LOG.info("Requesting new {} access token", request.uri());
        var response = HttpClient
          .newHttpClient()
          .send(request, HttpResponse.BodyHandlers.ofInputStream());
        var mapper = new ObjectMapper();
        var token = mapper.readValue(response.body(), OAuthToken.class);

        this.cachedToken = new CachedOAuthToken(token);

        LOG.info("Received new access token from URL {}", request.uri());
      } catch (IOException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    return cachedToken.value;
  }

  protected abstract HttpRequest oauthTokenRequest();
}
