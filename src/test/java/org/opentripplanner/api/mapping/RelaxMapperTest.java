package org.opentripplanner.api.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.preference.Relax;

class RelaxMapperTest {

  @Test
  void mapRelax() {
    assertNull(RelaxMapper.mapRelax(""));
    assertNull(RelaxMapper.mapRelax(null));

    Relax expected = new Relax(2.0, 10);

    assertEquals(expected, RelaxMapper.mapRelax("2 * generalizedCost + 10"));
    assertEquals(expected, RelaxMapper.mapRelax("10 + 2 * generalizedCost"));
    assertEquals(expected, RelaxMapper.mapRelax("2.0x+10"));
    assertEquals(expected, RelaxMapper.mapRelax("10+2.0x"));
    assertThrows(IllegalArgumentException.class, () -> RelaxMapper.mapRelax("2.0"));
    assertThrows(IllegalArgumentException.class, () -> RelaxMapper.mapRelax("2.0+10"));
    assertThrows(IllegalArgumentException.class, () -> RelaxMapper.mapRelax("2.0*10"));
  }
}
