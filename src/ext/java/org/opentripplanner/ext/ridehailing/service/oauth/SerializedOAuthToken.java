package org.opentripplanner.ext.ridehailing.service.oauth;

public record SerializedOAuthToken(String access_token, long expires_in) {}
