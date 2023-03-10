package org.opentripplanner.ext.carhailing.service.oauth;

import java.time.Duration;

public record OAuthToken(String value, Duration expiry) {}
