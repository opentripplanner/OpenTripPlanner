package org.opentripplanner.updater.spi;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

public class UpdateResultAssertions {

  public static void assertFailure(UpdateError.UpdateErrorType expectedError, UpdateResult result) {
    assertEquals(Set.of(expectedError), result.failures().keySet());
  }

  public static void assertNoFailure(UpdateResult result) {
    assertThat(result.failures().keySet()).isEmpty();
  }

  public static void assertSuccess(UpdateResult updateResult) {
    var errorCodes = updateResult.failures().keySet();
    assertEquals(
      Set.of(),
      errorCodes,
      "Update result should have no error codes but had %s".formatted(errorCodes)
    );
    assertTrue(updateResult.successful() > 0);
  }
}
