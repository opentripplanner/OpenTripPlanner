package org.opentripplanner.framework.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.OtpConfigLoader;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class OTPFeatureTest {

  private final OTPFeature subject = OTPFeature.GtfsGraphQlApi;

  @Test
  public void on() {
    subject.testOn(() -> {
      assertTrue(subject.isOn());
      assertFalse(subject.isOff());
    });
  }

  @Test
  public void off() {
    subject.testOff(() -> {
      assertFalse(subject.isOn());
      assertTrue(subject.isOff());
    });
  }

  @Test
  public void isOnElseNull() {
    subject.testOn(() -> {
      // then expect value to be passed through
      assertEquals("OK", subject.isOnElseNull(() -> "OK"));
    });
    subject.testOff(() -> {
      // then expect supplier to be ignored
      assertNull(subject.isOnElseNull(() -> Integer.parseInt("THROW EXCEPTION")));
    });
  }

  @Test
  public void allowOTPFeaturesToBeConfigurableFromJSON() {
    // Use a mapper to create a JSON configuration
    ObjectMapper mapper = JsonMapper.builder()
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
      .build();

    // Given the following config
    String json = """
    {
      otpFeatures : {
        GtfsGraphQlApi: false,
        MinimumTransferTimeIsDefinitive : true
      }
    }
    """;

    var configLoader = OtpConfigLoader.fromString(json);
    var config = configLoader.loadOtpConfig();
    // When
    OTPFeature.enableFeatures(config.otpFeatures);

    // Then
    assertTrue(OTPFeature.GtfsGraphQlApi.isOff());
    assertTrue(OTPFeature.MinimumTransferTimeIsDefinitive.isOn());
  }

  @Test
  public void doc() {
    assertEquals("Endpoint for actuators (service health status).", OTPFeature.ActuatorAPI.doc());
  }

  @Test
  public void isSandbox() {
    assertFalse(OTPFeature.APIServerInfo.isSandbox());
    assertTrue(OTPFeature.ActuatorAPI.isSandbox());
  }
}
