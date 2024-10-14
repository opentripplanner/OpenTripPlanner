package org.opentripplanner.ext.ridehailing.service.oauth;

import java.io.IOException;

/**
 * An interface for supplying an OAuth token to be used for further requests.
 */
public interface OAuthService {
  String getToken() throws IOException;
}
