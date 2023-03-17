package org.opentripplanner.ext.ridehailing.service.oauth;

import java.io.IOException;

public interface OAuthService {
  String getToken() throws IOException;
}
