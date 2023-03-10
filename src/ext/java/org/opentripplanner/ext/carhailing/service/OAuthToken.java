package org.opentripplanner.ext.carhailing.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Instant;

/**
 * Holds an OAuth access token and its expiration time for querying ride-hail APIs.
 */
public class OAuthToken {

  private static final ObjectMapper mapper = new ObjectMapper();

  public final String value;
  private Instant tokenExpirationTime;

  private OAuthToken() {
    value = null;
  }

  public static OAuthToken empty() {
    return new OAuthToken();
  }

  public OAuthToken(HttpURLConnection connection) throws IOException {
    // send request and parse response
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    String newValue;
    try (InputStream responseStream = connection.getInputStream()) {
      OAuthAuthenticationResponse response = mapper.readValue(
        responseStream,
        OAuthAuthenticationResponse.class
      );

      newValue = response.access_token;
      // Expire the token one minute before the actual expiry, e.g. to cover
      // long API calls or calls over slow networks near the expiration time.
      tokenExpirationTime = Instant.now().plusSeconds(response.expires_in - 60L);
    }
    value = newValue;
  }

  /**
   * Checks if a new token needs to be obtained.
   */
  public boolean isExpired() {
    return tokenExpirationTime == null || Instant.now().isAfter(tokenExpirationTime);
  }

  /**
   * Used for testing purposes only.
   */
  public void makeTokenExpire() {
    tokenExpirationTime = Instant.now().minusSeconds(1);
  }
}
