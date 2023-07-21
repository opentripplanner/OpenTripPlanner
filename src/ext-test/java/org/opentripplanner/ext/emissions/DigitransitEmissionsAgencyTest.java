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
    assertEquals("BUS", agency.getMode(TransitMode.BUS).getName());
  }

  @Test
  void testGetModNotNull() {
    assertNull(agency.getMode(TransitMode.AIRPLANE));
  }

  @Test
  void getAverageCo2EmissionsByModePerPerson() {
    assertEquals(10, this.agency.getAverageCo2EmissionsByModePerPerson("BUS"));
  }

  @Test
  void getAverageCo2EmissionsByModePerPersonNullMode() {
    assertEquals(0, this.agency.getAverageCo2EmissionsByModePerPerson("AIRPLANE"));
  }
}
