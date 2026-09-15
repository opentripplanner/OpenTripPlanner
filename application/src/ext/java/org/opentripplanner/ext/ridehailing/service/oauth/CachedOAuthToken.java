package org.opentripplanner.ext.ridehailing.service.oauth;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.ext.ridehailing.service.oauth.CachingOAuthService.SerializedOAuthToken;

/**
 * Holds an OAuth access token and its expiration time for querying ride-hail APIs.
 */
public class CachedOAuthToken {

  public final String value;
  private Instant tokenExpirationTime;

  private CachedOAuthToken() {
    value = null;
  }

  public static CachedOAuthToken empty() {
    return new CachedOAuthToken();
  }

  public CachedOAuthToken(SerializedOAuthToken token) {
    value = token.access_token();
    // Expire the token one minute before the actual expiry, e.g. to cover
    // long API calls or calls over slow networks near the expiration time.
    tokenExpirationTime = Instant.now()
      .plus(Duration.ofSeconds(token.expires_in()).minusMinutes(1));
  }

  /**
   * Checks if a new token needs to be obtained.
   */
  public boolean isExpired() {
    return tokenExpirationTime == null || Instant.now().isAfter(tokenExpirationTime);
  }
}
