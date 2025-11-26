package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.search.TraverseMode.BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.CAR;
import static org.opentripplanner.street.search.TraverseMode.SCOOTER;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.search.TraverseMode;

class StreetSearchRequestBuilderTest {

  private static final List<TraverseMode> RENTAL_MODES = List.of(BICYCLE, CAR, SCOOTER);

  @Test
  void withUseRentalAvailability() {
    var orig = StreetSearchRequest.of().build();
    RENTAL_MODES.forEach(m -> assertFalse(orig.rental(m).useAvailabilityInformation()));
    var modified = StreetSearchRequest.copyOf(orig).withUseRentalAvailability(true).build();
    RENTAL_MODES.forEach(m ->
      assertTrue(
        modified.rental(m).useAvailabilityInformation(),
        "Use rental availability for %s false, but should be true.".formatted(m)
      )
    );
  }
}
