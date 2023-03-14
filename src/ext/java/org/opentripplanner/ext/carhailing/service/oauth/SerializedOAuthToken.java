package org.opentripplanner.ext.carhailing.service.oauth;

public record SerializedOAuthToken(String access_token, long expires_in) {}
