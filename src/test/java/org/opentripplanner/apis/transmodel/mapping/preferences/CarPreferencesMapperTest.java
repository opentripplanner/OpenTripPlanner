package org.opentripplanner.apis.transmodel.mapping.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.transmodel._support.TestDataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.CarPreferences;

class CarPreferencesMapperTest {

  static List<Arguments> mapCarPreferencesTestCases() {
    return List.of(Arguments.of("walkReluctance", 7.5, "CarPreferences{reluctance: 7.5}"));
  }

  @ParameterizedTest
  @MethodSource("mapCarPreferencesTestCases")
  void mapCarPreferences(String field, Object value, String expected) {
    var preferences = CarPreferences.of();
    CarPreferencesMapper.mapCarPreferences(preferences, TestDataFetcherDecorator.of(field, value));
    assertEquals(expected, preferences.build().toString());
  }
}
