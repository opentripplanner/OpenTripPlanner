package org.opentripplanner.apis.transmodel.model.timetable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;

class EmpiricalDelayTypeTest {

  @Test
  void create() {
    var subject = EmpiricalDelayType.create();

    assertEquals(EmpiricalDelayType.NAME, subject.getName());
    assertThat(subject.getDescription()).isNotEmpty();

    var p50 = subject.getFieldDefinition("p50");
    assertNotNull(p50);
    assertEquals(TransmodelScalars.DURATION_SCALAR, p50.getType());

    var p90 = subject.getFieldDefinition("p90");
    assertNotNull(p90);
    assertEquals(TransmodelScalars.DURATION_SCALAR, p90.getType());
  }

  @Test
  void dataFetcherForTripTimeOnDate() {}
}
