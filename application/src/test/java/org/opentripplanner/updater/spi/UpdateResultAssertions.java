package org.opentripplanner.updater.spi;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.function.Supplier;

public class UpdateResultAssertions {

  public static void assertFailure(UpdateErrorType expectedError, UpdateResult result) {
    assertEquals(Set.of(expectedError), result.failures().keySet());
  }

  public static void assertNoFailure(UpdateResult result) {
    assertThat(result.failures().keySet()).isEmpty();
  }

  public static <T> UpdateError assertFailure(UpdateErrorType expectedError, Supplier<T> callback) {
    return assertFailure(expectedError, callback, "Expected failure with " + expectedError);
  }

  public static <T> UpdateError assertFailure(
    UpdateErrorType expectedError,
    Supplier<T> callback,
    String msg
  ) {
    UpdateException exn = assertThrows(UpdateException.class, callback::get, msg);

    assertEquals(expectedError, exn.errorType(), msg);

    return exn.toError();
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
