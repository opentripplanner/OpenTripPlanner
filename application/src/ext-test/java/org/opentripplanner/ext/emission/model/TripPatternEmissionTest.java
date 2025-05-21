package org.opentripplanner.ext.emission.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Emission;

class TripPatternEmissionTest {

  private static final Emission EM_HOP_1 = Emission.ofCo2Gram(2.5);
  private static final Emission EM_HOP_2 = Emission.ofCo2Gram(7.5);
  private static final Emission EM_HOP_3 = Emission.ofCo2Gram(2.0);
  private static final Emission EM_HOP_1_2 = EM_HOP_1.plus(EM_HOP_2);
  private static final Emission EM_HOP_2_3 = EM_HOP_2.plus(EM_HOP_3);
  private static final Emission EM_HOP_1_3 = EM_HOP_1_2.plus(EM_HOP_3);

  @Test
  void section() {
    var subject = new TripPatternEmission(List.of(EM_HOP_1, EM_HOP_2, EM_HOP_3));

    assertEquals(Emission.ZERO, subject.section(0, 0));
    assertEquals(EM_HOP_1, subject.section(0, 1));
    assertEquals(EM_HOP_2, subject.section(1, 2));
    assertEquals(EM_HOP_3, subject.section(2, 3));
    assertEquals(EM_HOP_1_2, subject.section(0, 2));
    assertEquals(EM_HOP_2_3, subject.section(1, 3));
    assertEquals(EM_HOP_1_3, subject.section(0, 3));
  }
}
