package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.SubMode;

class SubModeTest {

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCacheForever("localBus");
  private static final SubMode NIGHT_BUS = SubMode.getOrBuildAndCacheForever("nightBus");

  @Test
  void of() {
    assertSame(LOCAL_BUS, SubMode.of("localBus"));

    SubMode submode = SubMode.of("other");
    assertEquals(1_000_000, submode.index());
    assertEquals("other", submode.name());
  }

  @Test
  void build() {
    SubMode subject = SubMode.getOrBuildAndCacheForever("localBus");

    assertSame(LOCAL_BUS, subject);
    assertEquals("localBus", subject.name());
  }

  @Test
  void getByIndex() {
    BitSet bs = new BitSet();

    bs.set(NIGHT_BUS.index());
    assertEquals(Set.of(NIGHT_BUS), SubMode.getByIndex(bs));

    bs.set(LOCAL_BUS.index());
    assertEquals(Set.of(LOCAL_BUS, NIGHT_BUS), SubMode.getByIndex(bs));
  }

  @Test
  void index() {
    int index = LOCAL_BUS.index();
    assertTrue(index > 0 && index < 1_000, "Index: " + index);

    SubMode other1 = SubMode.of("other1");
    SubMode other2 = SubMode.of("other2");
    assertEquals(other1.index(), other2.index());
  }

  @Test
  void testEquals() {
    assertEquals(NIGHT_BUS, SubMode.of("nightBus"));
    assertNotEquals(SubMode.UNKNOWN, NIGHT_BUS);

    SubMode original = SubMode.of("not-cached");
    SubMode other = SubMode.of("not-cached");
    SubMode different = SubMode.of("not-cached-different");

    assertEquals(original, other);
    assertNotEquals(original, different);
  }

  @Test
  void testHashCode() {
    assertEquals(SubMode.of("nightBus").hashCode(), NIGHT_BUS.hashCode());
    assertEquals(SubMode.of("not-cached").hashCode(), SubMode.of("not-cached").hashCode());
    assertNotEquals(LOCAL_BUS.hashCode(), NIGHT_BUS.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("localBus", LOCAL_BUS.name());
  }
}
