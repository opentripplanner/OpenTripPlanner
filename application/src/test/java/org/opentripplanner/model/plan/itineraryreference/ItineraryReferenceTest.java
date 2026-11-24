package org.opentripplanner.model.plan.itineraryreference;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.street.model.StreetMode;

class ItineraryReferenceTest {

  private static final ScheduledTransitLegReference LEG = new ScheduledTransitLegReference(
    id("Trip"),
    LocalDate.of(2022, 1, 31),
    0,
    1,
    id("A"),
    id("B"),
    null
  );

  @Test
  void equalReferencesHaveEqualHashCodes() {
    var a = reference(List.of(LEG), 1.3);
    var b = reference(List.of(LEG), 1.3);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void referencesDifferingInOneFieldAreNotEqual() {
    assertNotEquals(reference(List.of(LEG), 1.3), reference(List.of(LEG), 1.4));
  }

  @Test
  void legReferencesAreDefensivelyCopied() {
    var legs = new ArrayList<LegReference>(List.of(LEG));
    var reference = reference(legs, 1.3);

    legs.add(LEG);

    assertThat(reference.legReferences()).containsExactly(LEG);
    assertThrows(UnsupportedOperationException.class, () -> reference.legReferences().add(LEG));
  }

  @Test
  void emptyLegReferencesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> reference(List.of(), 1.3));
  }

  @Test
  void toStringListsTheFields() {
    assertThat(reference(List.of(LEG), 1.3).toString()).contains("walkSpeed: 1.3");
  }

  /** Without them, a refetch would quietly use the server's preferences for the rental mode. */
  @Test
  void rentalModeRequiresItsPreferences() {
    assertThrows(IllegalArgumentException.class, () ->
      carRentalEgress(StreetMode.CAR_RENTAL, null)
    );
  }

  @Test
  void rentalPreferencesRequireTheirRentalMode() {
    var car = new CarRentalPreferences(2.0, new RentalNetworks(Set.of(), Set.of()));

    assertThrows(IllegalArgumentException.class, () -> carRentalEgress(StreetMode.WALK, car));
  }

  private static ItineraryReference carRentalEgress(
    StreetMode egressMode,
    CarRentalPreferences car
  ) {
    return new ItineraryReference(
      List.of(LEG),
      null,
      null,
      StreetMode.WALK,
      egressMode,
      StreetMode.WALK,
      Duration.ZERO,
      Duration.ZERO,
      1.3,
      2.0,
      1.0,
      false,
      null,
      car,
      null
    );
  }

  private static ItineraryReference reference(List<LegReference> legs, double walkSpeed) {
    return new ItineraryReference(
      legs,
      null,
      null,
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      Duration.ZERO,
      Duration.ZERO,
      walkSpeed,
      2.0,
      1.0,
      false,
      null,
      null,
      null
    );
  }
}
