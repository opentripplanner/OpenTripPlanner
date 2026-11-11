package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatchingParameters;

class SiriFuzzyTripMatchingConfigTest {

  @Test
  void missing() {
    assertEquals(SiriFuzzyTripMatchingParameters.DISABLED, map("{}"));
  }

  @Test
  void booleanFalse() {
    assertEquals(SiriFuzzyTripMatchingParameters.DISABLED, map("{ fuzzyTripMatching : false }"));
  }

  @Test
  void booleanTrue() {
    assertEquals(SiriFuzzyTripMatchingParameters.EXACT, map("{ fuzzyTripMatching : true }"));
  }

  @Test
  void emptyObject() {
    assertEquals(SiriFuzzyTripMatchingParameters.EXACT, map("{ fuzzyTripMatching : {} }"));
  }

  @Test
  void timeTolerance() {
    assertEquals(
      SiriFuzzyTripMatchingParameters.ofTimeTolerance(Duration.ofSeconds(59)),
      map("{ fuzzyTripMatching : { timeTolerance : '59s' } }")
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { "'yes'", "1", "[true]" })
  void invalidType(String value) {
    assertThrows(OtpAppException.class, () -> map("{ fuzzyTripMatching : " + value + " }"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "-1s", "30m", "PT0.5S", "PT59.5S" })
  void invalidTimeTolerance(String value) {
    assertThrows(OtpAppException.class, () ->
      map("{ fuzzyTripMatching : { timeTolerance : '" + value + "' } }")
    );
  }

  @Test
  void noUnusedParameterWarnings() {
    for (var json : List.of(
      "{ fuzzyTripMatching : true }",
      "{ fuzzyTripMatching : { timeTolerance : '59s' } }"
    )) {
      var root = newNodeAdapterForTest(json);
      SiriFuzzyTripMatchingConfig.map(root, V2_0);
      var warnings = new ArrayList<String>();
      root.logAllWarnings(warnings::add);
      assertEquals(List.of(), warnings, json);
    }
  }

  @Test
  void unknownParameterInObjectIsReported() {
    var root = newNodeAdapterForTest("{ fuzzyTripMatching : { tolerance : '59s' } }");
    SiriFuzzyTripMatchingConfig.map(root, V2_0);
    var warnings = new ArrayList<String>();
    root.logAllWarnings(warnings::add);
    assertEquals(1, warnings.size(), warnings.toString());
  }

  private static SiriFuzzyTripMatchingParameters map(String json) {
    return SiriFuzzyTripMatchingConfig.map(newNodeAdapterForTest(json), V2_0);
  }
}
