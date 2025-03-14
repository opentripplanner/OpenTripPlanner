package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;

class RoutingResultTest implements PlanTestConstants {

  private static final Itinerary I1 = TestItineraryBuilder.newItinerary(A, 0)
    .walk(120, B)
    .bus(12, 240, 360, C)
    .build();
  private static final Itinerary I2 = TestItineraryBuilder.newItinerary(A, 0)
    .bus(5, 40, 460, C)
    .build();
  private static final RoutingError E1 = new RoutingError(
    RoutingErrorCode.OUTSIDE_SERVICE_PERIOD,
    InputField.DATE_TIME
  );
  private static final RoutingError E2 = new RoutingError(
    RoutingErrorCode.NO_TRANSIT_CONNECTION,
    InputField.FROM_PLACE
  );

  @Test
  void empty() {
    var subject = RoutingResult.empty();
    assertTrue(subject.itineraries().isEmpty());
    assertTrue(subject.errors().isEmpty());
  }

  @Test
  void ok() {
    var subject = RoutingResult.ok(List.of(I1));
    assertEquals(I1, subject.itineraries().getFirst());
    assertTrue(subject.errors().isEmpty());
    assertContains(subject, List.of(I1), Set.of());
  }

  @Test
  void failed() {
    var subject = RoutingResult.failed(List.of(E1));
    assertTrue(subject.itineraries().isEmpty());
    assertEquals(E1, subject.errors().stream().findFirst().orElseThrow());
    assertContains(subject, List.of(), Set.of(E1));
  }

  @Test
  void merge() {
    var subject = RoutingResult.empty();
    assertContains(subject, List.of(), Set.of());

    subject.merge(RoutingResult.ok(List.of(I1)));
    assertContains(subject, List.of(I1), Set.of());

    subject.merge(RoutingResult.failed(Set.of(E1)));
    assertContains(subject, List.of(I1), Set.of(E1));

    subject.merge(RoutingResult.failed(Set.of(E2)), RoutingResult.ok(List.of(I2)));
    assertContains(subject, List.of(I1, I2), Set.of(E1, E2));

    // Merrging in the same error twice have no effect, also the order is not important
    subject.merge(RoutingResult.failed(Set.of(E2)));
    assertContains(subject, List.of(I1, I2), Set.of(E2, E1));
  }

  @Test
  void transform() {
    var subject = RoutingResult.ok(List.of(I1));

    subject.transform(l -> List.of(l.getFirst(), I2));
    assertContains(subject, List.of(I1, I2), Set.of());

    subject.transform(l -> List.of(I2));
    assertContains(subject, List.of(I2), Set.of());
  }

  void addErrors() {
    var subject = RoutingResult.empty();

    subject.addErrors(List.of(E1));
    assertContains(subject, List.of(), Set.of(E1));

    subject.addErrors(Set.of(E2));
    assertContains(subject, List.of(), Set.of(E1, E2));
  }

  private void assertContains(
    RoutingResult subject,
    List<Itinerary> expItineraries,
    Set<RoutingError> expErrors
  ) {
    assertEquals(expItineraries, subject.itineraries());
    assertEquals(expErrors, subject.errors());
  }
}
