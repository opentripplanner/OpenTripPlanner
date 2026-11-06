package org.opentripplanner.model.plan.itineraryreference;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.street.model.StreetMode;

class ItineraryReferenceTest {

  private static final ScheduledTransitLegReference LEG = new ScheduledTransitLegReference(
    FeedScopedIdForTestFactory.id("Trip"),
    LocalDate.of(2022, 1, 31),
    0,
    1,
    FeedScopedIdForTestFactory.id("A"),
    FeedScopedIdForTestFactory.id("B"),
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
      false
    );
  }
}
