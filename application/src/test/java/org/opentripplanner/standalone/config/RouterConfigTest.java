package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RouterConfigTest {

  @Test
  void defaultMaxSearchWindowIs24Hours() {
    validateMaxSearchWindow("", Duration.ofHours(24));
  }

  @Test
  void maxSearchWindowIsOverriddenInRouterConfig() {
    validateMaxSearchWindow(
      """
      {
        "transit": {
          "maxSearchWindow": "48h"
        }
      }
      """,
      Duration.ofHours(48)
    );
  }

  private void validateMaxSearchWindow(String configuration, Duration expectedDuration) {
    RouterConfig routerConfig = new RouterConfig(newNodeAdapterForTest(configuration), false);
    assertEquals(expectedDuration, routerConfig.routingRequestDefaults().maxSearchWindow());
  }
}
