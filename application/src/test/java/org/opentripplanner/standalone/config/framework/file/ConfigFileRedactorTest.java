package org.opentripplanner.standalone.config.framework.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class ConfigFileRedactorTest {

  private static final String REDACTED = "********";

  @Test
  void redactSecretsInFlatObject() {
    JsonNode node = parse(
      """
      {
        "secretKey": "mySecret",
        "accessKey": "myAccessKey",
        "gsCredentials": "myCredentials",
        "password": "myPassword",
        "normalKey": "visible"
      }
      """
    );

    JsonNode redacted = parseRedacted(node);

    assertEquals(REDACTED, redacted.path("secretKey").asText());
    assertEquals(REDACTED, redacted.path("accessKey").asText());
    assertEquals(REDACTED, redacted.path("gsCredentials").asText());
    assertEquals(REDACTED, redacted.path("password").asText());
    assertEquals("visible", redacted.path("normalKey").asText());
  }

  @Test
  void redactSecretsInNestedObject() {
    JsonNode node = parse(
      """
      {
        "storage": {
          "secretKey": "nested-secret",
          "bucket": "my-bucket"
        }
      }
      """
    );

    JsonNode redacted = parseRedacted(node);

    assertEquals(REDACTED, redacted.path("storage").path("secretKey").asText());
    assertEquals("my-bucket", redacted.path("storage").path("bucket").asText());
  }

  @Test
  void redactSecretsInArray() {
    JsonNode node = parse(
      """
      {
        "updaters": [
          { "type": "siri", "secretKey": "abc123" },
          { "type": "gtfs-rt", "password": "pass456" }
        ]
      }
      """
    );

    JsonNode redacted = parseRedacted(node);

    JsonNode updaters = redacted.path("updaters");
    assertEquals("siri", updaters.get(0).path("type").asText());
    assertEquals(REDACTED, updaters.get(0).path("secretKey").asText());
    assertEquals("gtfs-rt", updaters.get(1).path("type").asText());
    assertEquals(REDACTED, updaters.get(1).path("password").asText());
  }

  @Test
  void redactSecretsInDeeplyNestedStructure() {
    JsonNode node = parse(
      """
      {
        "services": [
          {
            "name": "service-a",
            "endpoints": [
              { "url": "https://example.com", "accessKey": "deep-secret" }
            ]
          }
        ]
      }
      """
    );

    JsonNode redacted = parseRedacted(node);

    JsonNode endpoint = redacted.path("services").get(0).path("endpoints").get(0);
    assertEquals("https://example.com", endpoint.path("url").asText());
    assertEquals(REDACTED, endpoint.path("accessKey").asText());
  }

  @Test
  void nonSecretValuesArePreserved() {
    JsonNode node = parse(
      """
      {
        "name": "test",
        "count": 42,
        "enabled": true,
        "items": ["a", "b"]
      }
      """
    );

    JsonNode redacted = parseRedacted(node);

    assertEquals("test", redacted.path("name").asText());
    assertEquals(42, redacted.path("count").asInt());
    assertEquals(true, redacted.path("enabled").asBoolean());
    assertEquals("a", redacted.path("items").get(0).asText());
    assertEquals("b", redacted.path("items").get(1).asText());
  }

  @Test
  void originalNodeIsNotModified() {
    JsonNode node = parse(
      """
      { "secretKey": "original-value" }
      """
    );

    ConfigFileRedactor.toRedactedString(node);

    assertEquals("original-value", node.path("secretKey").asText());
  }

  private static JsonNode parse(String json) {
    return ConfigFileLoader.nodeFromString(json, "test");
  }

  private static JsonNode parseRedacted(JsonNode node) {
    String redactedString = ConfigFileRedactor.toRedactedString(node);
    return ConfigFileLoader.nodeFromString(redactedString, "test");
  }
}
