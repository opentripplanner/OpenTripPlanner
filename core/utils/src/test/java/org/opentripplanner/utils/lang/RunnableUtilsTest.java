package org.opentripplanner.utils.lang;

import org.junit.jupiter.api.Test;

class RunnableUtilsTest {

  @Test
  void runningNOOPShouldNotThrow() {
    RunnableUtils.NOOP.run();
  }
}
