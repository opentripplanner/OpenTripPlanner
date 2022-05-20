package org.opentripplanner.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.standalone.configure.OTPConfiguration;

public class OTPFeatureTest {

  private final OTPFeature subject = OTPFeature.APIBikeRental;
  private final Map<OTPFeature, Boolean> backupValues = new HashMap<>();

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
            APIBikeRental : false,
            MinimumTransferTimeIsDefinitive : true
          }
        }
        """;

    OTPConfiguration config = OTPConfiguration.createForTest(json);

    // When
    OTPFeature.enableFeatures(config.otpConfig().otpFeatures);

    // Then
    assertTrue(OTPFeature.APIBikeRental.isOff());
    assertTrue(OTPFeature.MinimumTransferTimeIsDefinitive.isOn());
  }
}
