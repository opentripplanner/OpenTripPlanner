package org.opentripplanner.transfer.configure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegularTransferRepositoryModuleTest {

  @Test
  void warnIfElevatorStatusIsUpdatedWithoutAlternativePaths() {
    var warning = RegularTransferRepositoryModule.missingElevatorAlternativesWarning(false, 1);

    assertTrue(warning.isPresent());
    assertTrue(warning.get().contains("elevatorAlternatives"));
  }

  @Test
  void noWarningWithAlternativePaths() {
    assertEquals(
      Optional.empty(),
      RegularTransferRepositoryModule.missingElevatorAlternativesWarning(true, 1)
    );
  }

  @Test
  void noWarningWithoutElevatorStatusUpdater() {
    assertEquals(
      Optional.empty(),
      RegularTransferRepositoryModule.missingElevatorAlternativesWarning(false, 0)
    );
  }
}
