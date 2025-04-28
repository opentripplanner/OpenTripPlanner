package org.opentripplanner.ext.emission.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.emission.model.CarEmissionUtil.calculateCarCo2EmissionPerMeterPerPerson;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.emission.parameters.EmissionViechleParameters;
import org.opentripplanner.framework.model.Gram;

class CarEmissionUtilTest {

  @Test
  void testCalculateCarCo2EmissionPerMeterPerPerson() {
    assertEquals(
      Gram.of(0.08),
      calculateCarCo2EmissionPerMeterPerPerson(new EmissionViechleParameters(Gram.of(160.0), 2.0))
    );
  }
}
