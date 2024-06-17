package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;

public class UpdateResultAssertions {

  public static void assertFailure(UpdateError.UpdateErrorType expectedError, UpdateResult result) {
    assertEquals(Set.of(expectedError), result.failures().keySet());
  }

  public static UpdateResult assertSuccess(UpdateResult updateResult) {
    var errorCodes = updateResult.failures().keySet();
    assertEquals(
      Set.of(),
      errorCodes,
      "Update result should have no error codes but had %s".formatted(errorCodes)
    );
    assertTrue(updateResult.successful() > 0);
    return updateResult;
  }
}
