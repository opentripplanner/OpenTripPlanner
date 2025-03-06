package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;

class MultiCriteriaRequestTest {

  private static final RelaxFunction RELAX_C1 = GeneralizedCostRelaxFunction.of(2.0, 600);

  private final MultiCriteriaRequest<RaptorTripSchedule> subject = MultiCriteriaRequest.of()
    .withRelaxC1(RELAX_C1)
    .build();

  @Test
  void copyOf() {
    assertSame(subject, subject.copyOf().build());

    // Change a filed - build - make a new copy and set same value => should be equal
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
  void testEqualsAndHashCode() {
    var eq = MultiCriteriaRequest.of().withRelaxC1(RELAX_C1).build();
    var noRelaxC1 = subject.copyOf().withRelaxC1(RelaxFunction.NORMAL).build();

    assertEquals(subject, subject);
    assertEquals(subject, eq);
    assertNotEquals(subject, noRelaxC1);

    assertEquals(subject.hashCode(), eq.hashCode());
    assertNotEquals(subject.hashCode(), noRelaxC1.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("MultiCriteriaRequest{relaxC1: f(x) = 2.00 * x + 6.0}", subject.toString());
  }
}
