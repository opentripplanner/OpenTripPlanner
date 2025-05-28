package org.opentripplanner.framework.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.OtpConfigLoader;

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
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
    mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

    // Given the following config
    String json =
      """
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
