package org.opentripplanner.standalone.config.framework.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

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

    assertEquals(REDACTED, redacted.path("secretKey").asString());
    assertEquals(REDACTED, redacted.path("accessKey").asString());
    assertEquals(REDACTED, redacted.path("gsCredentials").asString());
    assertEquals(REDACTED, redacted.path("password").asString());
    assertEquals("visible", redacted.path("normalKey").asString());
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

    assertEquals(REDACTED, redacted.path("storage").path("secretKey").asString());
    assertEquals("my-bucket", redacted.path("storage").path("bucket").asString());
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
    assertEquals("siri", updaters.get(0).path("type").asString());
    assertEquals(REDACTED, updaters.get(0).path("secretKey").asString());
    assertEquals("gtfs-rt", updaters.get(1).path("type").asString());
    assertEquals(REDACTED, updaters.get(1).path("password").asString());
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
    assertEquals("https://example.com", endpoint.path("url").asString());
    assertEquals(REDACTED, endpoint.path("accessKey").asString());
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

    assertEquals("test", redacted.path("name").asString());
    assertEquals(42, redacted.path("count").asInt());
    assertEquals(true, redacted.path("enabled").asBoolean());
    assertEquals("a", redacted.path("items").get(0).asString());
    assertEquals("b", redacted.path("items").get(1).asString());
  }

  @Test
  void originalNodeIsNotModified() {
    JsonNode node = parse(
      """
      { "secretKey": "original-value" }
      """
    );

    ConfigFileRedactor.toRedactedString(node);

    assertEquals("original-value", node.path("secretKey").asString());
  }

  private static JsonNode parse(String json) {
    return ConfigFileLoader.nodeFromString(json, "test");
  }

  private static JsonNode parseRedacted(JsonNode node) {
    String redactedString = ConfigFileRedactor.toRedactedString(node);
    return ConfigFileLoader.nodeFromString(redactedString, "test");
  }
}
