package org.opentripplanner.ext.carhailing.service.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.opentripplanner.ext.carhailing.CarHailingService;
import org.opentripplanner.framework.json.ObjectMappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OAuthService {

  private static ObjectMapper MAPPER = ObjectMappers.ignoringExtraFields();
  private static final Logger LOG = LoggerFactory.getLogger(CarHailingService.class);
  private CachedOAuthToken cachedToken = CachedOAuthToken.empty();

  /**
   * Obtains and caches an OAuth API access token.
   * @return A token holder with the token value (including null token values if the call was unsuccessful).
   */
  public String getToken() throws IOException {
    if (cachedToken.isExpired()) {
      // prepare request to get token
      try {
        var request = oauthTokenRequest();
        LOG.info("Requesting new {} access token", request.uri());
        var response = HttpClient
          .newHttpClient()
          .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
          LOG.error(
            "Error fetching OAuth token from {}. Response: {}",
            request.uri(),
            response.body()
          );
          throw new IOException("Could not fetch OAuth token from %s".formatted(request.uri()));
        }

        var token = MAPPER.readValue(response.body(), SerializedOAuthToken.class);
        this.cachedToken = new CachedOAuthToken(token);

        LOG.info("Received new access token from URL {}", request.uri());
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }

    return cachedToken.value;
  }

  protected abstract HttpRequest oauthTokenRequest();
}
