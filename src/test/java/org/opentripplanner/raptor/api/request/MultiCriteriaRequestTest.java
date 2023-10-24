package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;

class MultiCriteriaRequestTest {

  private static final RelaxFunction RELAX_C1 = GeneralizedCostRelaxFunction.of(2.0, 600);

  private static final List<PassThroughPoint> PASS_THROUGH_POINTS = List.of(
    new PassThroughPoint(null, 7, 13)
  );

  private final MultiCriteriaRequest<RaptorTripSchedule> subject = MultiCriteriaRequest
    .of()
    .withRelaxC1(RELAX_C1)
    .withPassThroughPoints(PASS_THROUGH_POINTS)
    .build();

  @Test
  void copyOf() {
    assertSame(subject, subject.copyOf().build());

    // Change a filed - build - make a new copy and set same value => should be equal
    assertEquals(
      subject,
      subject
        .copyOf()
        .withPassThroughPoints(null)
        .build()
        .copyOf()
        .withPassThroughPoints(PASS_THROUGH_POINTS)
        .build()
    );
    // Change another filed - build - make a new copy and set same value => should be equal
    assertEquals(
      subject,
      subject
        .copyOf()
        .withRelaxC1(RelaxFunction.NORMAL)
        .build()
        .copyOf()
        .withRelaxC1(RELAX_C1)
        .build()
    );
  }

  @Test
  void relaxC1() {
    assertEquals(RELAX_C1, subject.relaxC1());
  }

  @Test
  void passThroughPoints() {
    assertEquals(PASS_THROUGH_POINTS, subject.passThroughPoints());
  }

  @Test
  void testEqualsAndHashCode() {
    var eq = MultiCriteriaRequest
      .of()
      .withRelaxC1(RELAX_C1)
      .withPassThroughPoints(PASS_THROUGH_POINTS)
      .build();
    var noRelaxC1 = subject.copyOf().withRelaxC1(RelaxFunction.NORMAL).build();
    var noPassThroughPoint = subject.copyOf().withPassThroughPoints(null).build();

    assertEquals(subject, subject);
    assertEquals(subject, eq);
    assertNotEquals(subject, noRelaxC1);
    assertNotEquals(subject, noPassThroughPoint);

    assertEquals(subject.hashCode(), eq.hashCode());
    assertNotEquals(subject.hashCode(), noRelaxC1.hashCode());
    assertNotEquals(subject.hashCode(), noPassThroughPoint.hashCode());
  }

  @Test
  void testToString() {
    assertEquals(
      "MultiCriteriaRequest{relaxC1: f(x) = 2.00 * x + 6.0, passThroughPoints: [(stops: 7, 13)]}",
      subject.toString()
    );
  }

  @Test
  void includeC2() {
    assertTrue(subject.includeC2());
    assertFalse(MultiCriteriaRequest.of().build().includeC2());
    assertTrue(
      MultiCriteriaRequest.of().withPassThroughPoints(PASS_THROUGH_POINTS).build().includeC2()
    );
    assertFalse(MultiCriteriaRequest.of().withRelaxC1(RELAX_C1).build().includeC2());
  }
}
