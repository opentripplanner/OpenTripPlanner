package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.TransitMode;

class DigitransitEmissionsAgencyTest {

  private DigitransitEmissionsAgency agency;

  @BeforeEach
  protected void setUp() throws Exception {
    DigitransitEmissionsMode mode = new DigitransitEmissionsMode("BUS", "120", 12);
    this.agency = new DigitransitEmissionsAgency("db", "FOO", "FOO_COMP");

    this.agency.addMode(mode);
  }

  @Test
  void testGetModeDefaultBehavior() {
    assertEquals("BUS", this.agency.getMode(TransitMode.BUS).getName());
  }

  @Test
  void testGetModeNull() {
    assertNull(this.agency.getMode(TransitMode.AIRPLANE));
  }

  @Test
  void getAverageCo2EmissionsByModeAndDistancePerPerson() {
    assertEquals(5, this.agency.getAverageCo2EmissionsByModeAndDistancePerPerson("BUS", 0.5));
  }

  @Test
  void getAverageCo2EmissionsByModeAndDistancePerPersonNullMode() {
    assertEquals(-1, this.agency.getAverageCo2EmissionsByModeAndDistancePerPerson("AIRPLANE", 1));
  }

  @Test
  void getAverageCo2EmissionsByModeAndDistancePerPersonZeroDistance() {
    assertEquals(0, this.agency.getAverageCo2EmissionsByModeAndDistancePerPerson("BUS", 0));
  }

  @Test
  void getAverageCo2EmissionsByModeAndDistancePerPersonNegativeDistance() {
    assertEquals(-1, this.agency.getAverageCo2EmissionsByModeAndDistancePerPerson("BUS", -1));
  }
}
