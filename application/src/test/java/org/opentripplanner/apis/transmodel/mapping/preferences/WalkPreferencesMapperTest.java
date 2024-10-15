package org.opentripplanner.apis.transmodel.mapping.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.transmodel._support.TestDataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;

class WalkPreferencesMapperTest {

  static List<Arguments> mapWalkPreferencesTestCases() {
    return List.of(
      Arguments.of("walkReluctance", 7.5, "WalkPreferences{reluctance: 7.5}"),
      Arguments.of("walkSpeed", 3.2, "WalkPreferences{speed: 3.2}")
    );
  }

  @ParameterizedTest
  @MethodSource("mapWalkPreferencesTestCases")
  void mapWalkPreferences(String field, Object value, String expected) {
    var preferences = WalkPreferences.of();
    WalkPreferencesMapper.mapWalkPreferences(
      preferences,
      TestDataFetcherDecorator.of(field, value)
    );
    assertEquals(expected, preferences.build().toString());
  }
}
