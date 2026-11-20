package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatchingParameters;

class SiriETMqttUpdaterConfigTest {

  private static final String REQUIRED_PARAMETERS = """
  feedId : 'feed', host : 'localhost', port : 1883, topic : 'et/#', qos : 1
  """;

  @Test
  void fuzzyTripMatchingIsOptional() {
    var parameters = SiriETMqttUpdaterConfig.create(
      "test",
      newNodeAdapterForTest("{ " + REQUIRED_PARAMETERS + " }")
    );
    assertEquals(SiriFuzzyTripMatchingParameters.DISABLED, parameters.fuzzyTripMatching());
  }

  @Test
  void fuzzyMatchingWithoutDeprecatedParameter() {
    var parameters = SiriETMqttUpdaterConfig.create(
      "test",
      newNodeAdapterForTest(
        "{ " + REQUIRED_PARAMETERS + ", fuzzyMatching : { timeTolerance : '59s' } }"
      )
    );
    assertEquals(
      SiriFuzzyTripMatchingParameters.ofTimeTolerance(Duration.ofSeconds(59)),
      parameters.fuzzyTripMatching()
    );
  }
}
