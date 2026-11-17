package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatchingParameters;

class SiriFuzzyTripMatchingConfigTest {

  private static final SiriFuzzyTripMatchingParameters TIME_TOLERANCE_59_SECONDS =
    SiriFuzzyTripMatchingParameters.ofTimeTolerance(Duration.ofSeconds(59));

  @Test
  void missingUsesDeprecatedParameter() {
    assertEquals(SiriFuzzyTripMatchingParameters.DISABLED, map("{}", false));
    assertEquals(SiriFuzzyTripMatchingParameters.EXACT, map("{}", true));
  }

  @Test
  void emptyObjectEnablesExactMatching() {
    assertEquals(SiriFuzzyTripMatchingParameters.EXACT, map("{ fuzzyMatching : {} }", false));
  }

  @Test
  void timeTolerance() {
    assertEquals(
      TIME_TOLERANCE_59_SECONDS,
      map("{ fuzzyMatching : { timeTolerance : '59s' } }", false)
    );
  }

  @Test
  void fuzzyMatchingOverridesDeprecatedParameter() {
    assertEquals(
      TIME_TOLERANCE_59_SECONDS,
      map("{ fuzzyMatching : { timeTolerance : '59s' } }", true)
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { "false", "true", "'yes'", "1", "[]", "[{}]", "null" })
  void nonObjectValueIsRejected(String value) {
    assertThrows(OtpAppException.class, () -> map("{ fuzzyMatching : " + value + " }", false));
    assertThrows(OtpAppException.class, () -> map("{ fuzzyMatching : " + value + " }", true));
  }

  @ParameterizedTest
  @ValueSource(strings = { "-1s", "30m", "PT0.5S", "PT59.5S" })
  void invalidTimeTolerance(String value) {
    assertThrows(OtpAppException.class, () ->
      map("{ fuzzyMatching : { timeTolerance : '" + value + "' } }", false)
    );
  }

  @Test
  void noUnusedParameterWarnings() {
    var root = newNodeAdapterForTest("{ fuzzyMatching : { timeTolerance : '59s' } }");
    SiriFuzzyTripMatchingConfig.map(root, false);
    var warnings = new ArrayList<String>();
    root.logAllWarnings(warnings::add);
    assertEquals(List.of(), warnings);
  }

  @Test
  void unknownParameterInObjectIsReported() {
    var root = newNodeAdapterForTest("{ fuzzyMatching : { tolerance : '59s' } }");
    SiriFuzzyTripMatchingConfig.map(root, false);
    var warnings = new ArrayList<String>();
    root.logAllWarnings(warnings::add);
    assertEquals(1, warnings.size(), warnings.toString());
  }

  private static SiriFuzzyTripMatchingParameters map(String json, boolean fuzzyTripMatching) {
    return SiriFuzzyTripMatchingConfig.map(newNodeAdapterForTest(json), fuzzyTripMatching);
  }
}
