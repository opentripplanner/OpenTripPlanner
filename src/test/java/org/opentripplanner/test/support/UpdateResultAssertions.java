package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;

public class UpdateResultAssertions {
  public static void assertFailure(UpdateError.UpdateErrorType expectedError, UpdateResult result) {
    assertEquals(Set.of(expectedError), result.failures().keySet());
  }
}
