package org.opentripplanner.transit.model.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MainAndSubModeTest {

  public static final MainAndSubMode BUS = new MainAndSubMode(TransitMode.BUS);
  public static final MainAndSubMode LOCAL_BUS = new MainAndSubMode(
    TransitMode.BUS,
    SubMode.of("LOCAL")
  );

  @Test
  public void testToString() {
    assertEquals("BUS", BUS.toString());
    assertEquals("BUS::LOCAL", LOCAL_BUS.toString());
  }

  @Test
  public void mainModeOnly() {
    assertTrue(BUS.isMainModeOnly(), BUS.toString());
    assertFalse(LOCAL_BUS.isMainModeOnly(), LOCAL_BUS.toString());
  }

  @Test
  public void notMainModes() {
    assertEquals(MainAndSubMode.all(), MainAndSubMode.notMainModes(List.of()));
    assertEquals(
      MainAndSubMode.all().stream().filter(m -> !BUS.equals(m)).toList(),
      MainAndSubMode.notMainModes(List.of(BUS))
    );
  }

  @Test
  public void listToString() {
    assertEquals("[]", MainAndSubMode.toString(List.of()));
    assertEquals("[BUS]", MainAndSubMode.toString(List.of(BUS)));
    assertEquals("[BUS, BUS::LOCAL]", MainAndSubMode.toString(List.of(LOCAL_BUS, BUS)));
  }
}
