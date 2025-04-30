package org.opentripplanner.ext.emission.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;

class EmissionViechleParametersTest {

  private static final int AVG_CO_2_PER_KM = 90;
  private static final double AVG_OCCUPANCY = 7.4;

  private final EmissionViechleParameters subject = new EmissionViechleParameters(
    AVG_CO_2_PER_KM,
    AVG_OCCUPANCY
  );

  @Test
  void testToString() {
    assertEquals(
      "EmissionViechleParameters{carAvgCo2PerKm: 90, carAvgOccupancy: 7.4}",
      subject.toString()
    );
  }

  @Test
  void avgCo2PerKm() {
    assertEquals(AVG_CO_2_PER_KM, subject.avgCo2PerKm());
  }

  @Test
  void avgOccupancy() {
    assertEquals(AVG_OCCUPANCY, subject.avgOccupancy());
  }

  @Test
  void testEqualsAndHashCode() {
    var same = new EmissionViechleParameters(AVG_CO_2_PER_KM, AVG_OCCUPANCY);
    var otherOccupancy = new EmissionViechleParameters(
      AVG_CO_2_PER_KM,
      subject.avgOccupancy() + 1.0
    );
    var otherCO2 = new EmissionViechleParameters(subject.avgCo2PerKm() + 1, AVG_OCCUPANCY);

    AssertEqualsAndHashCode.verify(subject).sameAs(same).differentFrom(otherOccupancy, otherCO2);
  }
}
