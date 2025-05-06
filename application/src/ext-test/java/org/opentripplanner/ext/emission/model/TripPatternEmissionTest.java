package org.opentripplanner.ext.emission.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Emission;

class TripPatternEmissionTest {

  private static final Emission EM_LEG_1 = Emission.co2_g(2.5);
  private static final Emission EM_LEG_2 = Emission.co2_g(7.5);
  private static final Emission EM_LEG_3 = Emission.co2_g(2.0);
  private static final Emission EM_LEG_1_2 = EM_LEG_1.plus(EM_LEG_2);
  private static final Emission EM_LEG_2_3 = EM_LEG_2.plus(EM_LEG_3);
  private static final Emission EM_LEG_1_3 = EM_LEG_1_2.plus(EM_LEG_3);

  @Test
  void subsection() {
    var subject = new TripPatternEmission(List.of(EM_LEG_1, EM_LEG_2, EM_LEG_3));

    assertEquals(Emission.ZERO, subject.subsection(0, 0));
    assertEquals(EM_LEG_1, subject.subsection(0, 1));
    assertEquals(EM_LEG_2, subject.subsection(1, 2));
    assertEquals(EM_LEG_3, subject.subsection(2, 3));
    assertEquals(EM_LEG_1_2, subject.subsection(0, 2));
    assertEquals(EM_LEG_2_3, subject.subsection(1, 3));
    assertEquals(EM_LEG_1_3, subject.subsection(0, 3));
  }
}
