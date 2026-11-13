package org.opentripplanner.ext.emission.model;

import static com.google.common.truth.Truth.assertThat;

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

    assertThat(subject.section(0, 0)).hasValue(Emission.ZERO);
    assertThat(subject.section(0, 1)).hasValue(EM_HOP_1);
    assertThat(subject.section(1, 2)).hasValue(EM_HOP_2);
    assertThat(subject.section(2, 3)).hasValue(EM_HOP_3);
    assertThat(subject.section(0, 2)).hasValue(EM_HOP_1_2);
    assertThat(subject.section(1, 3)).hasValue(EM_HOP_2_3);
    assertThat(subject.section(0, 3)).hasValue(EM_HOP_1_3);
  }

  @Test
  void sectionOutsideHops() {
    var subject = new TripPatternEmission(List.of(EM_HOP_1, EM_HOP_2, EM_HOP_3));

    assertThat(subject.section(1, 4)).isEmpty();
    assertThat(subject.section(4, 5)).isEmpty();
  }
}
