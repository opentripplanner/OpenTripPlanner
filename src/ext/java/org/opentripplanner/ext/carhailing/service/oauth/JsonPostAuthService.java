package org.opentripplanner.ext.carhailing.service.oauth;

import static java.util.Map.entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriBuilder;
import java.net.http.HttpRequest;
import java.util.Map;

public class JsonPostAuthService extends OAuthService {

  private static final ObjectMapper mapper = new ObjectMapper();
  private final String clientSecret;
  private final String clientId;
  private final String baseUrl;

  public JsonPostAuthService(String clientSecret, String clientId, String baseUrl) {
    this.clientSecret = clientSecret;
    this.clientId = clientId;
    this.baseUrl = baseUrl;
  }

  @Override
  protected HttpRequest oauthTokenRequest() {
    var uri = UriBuilder.fromUri(baseUrl + "oauth/token").build();
    String userpass = clientId + ":" + clientSecret;
    String basicAuth =
      "Basic " + jakarta.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

    var body = Map.ofEntries(entry("grant_type", "client_credentials"), entry("scope", "public"));
    try {
      var bodyAsString = mapper.writeValueAsString(body);
      return HttpRequest
        .newBuilder(uri)
        .header("Authorization", basicAuth)
        .header("Content-Type", "application/json;charset=UTF-8")
        .POST(HttpRequest.BodyPublishers.ofString(bodyAsString))
        .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
