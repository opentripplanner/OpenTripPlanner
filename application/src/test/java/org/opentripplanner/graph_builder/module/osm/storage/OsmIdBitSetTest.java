package org.opentripplanner.graph_builder.module.osm.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.carrotsearch.hppc.LongArrayList;
import org.junit.jupiter.api.Test;

class OsmIdBitSetTest {

  @Test
  void emptySetContainsNothing() {
    var subject = new OsmIdBitSet();
    assertFalse(subject.contains(0));
    assertFalse(subject.contains(1));
    assertFalse(subject.contains(Long.MAX_VALUE));
  }

  @Test
  void containsAddedIdsOnly() {
    var subject = new OsmIdBitSet();
    subject.add(0);
    subject.add(63);
    subject.add(64);
    subject.add(12_345_678_901L);

    assertTrue(subject.contains(0));
    assertTrue(subject.contains(63));
    assertTrue(subject.contains(64));
    assertTrue(subject.contains(12_345_678_901L));

    assertFalse(subject.contains(1));
    assertFalse(subject.contains(62));
    assertFalse(subject.contains(65));
    assertFalse(subject.contains(127));
    assertFalse(subject.contains(12_345_678_900L));
    assertFalse(subject.contains(12_345_678_902L));
  }

  @Test
  void idsSharingAWordDoNotOverwriteEachOther() {
    var subject = new OsmIdBitSet();
    for (long id = 128; id < 192; id += 2) {
      subject.add(id);
    }
    for (long id = 128; id < 192; id++) {
      if (id % 2 == 0) {
        assertTrue(subject.contains(id), "contains " + id);
      } else {
        assertFalse(subject.contains(id), "does not contain " + id);
      }
    }
  }

  @Test
  void addingTwiceIsIdempotent() {
    var subject = new OsmIdBitSet();
    subject.add(42);
    subject.add(42);
    assertTrue(subject.contains(42));
    assertFalse(subject.contains(43));
  }

  @Test
  void addAll() {
    var subject = new OsmIdBitSet();
    subject.addAll(LongArrayList.from(5, 70, 1_000_000_000_000L));

    assertTrue(subject.contains(5));
    assertTrue(subject.contains(70));
    assertTrue(subject.contains(1_000_000_000_000L));
    assertFalse(subject.contains(6));
  }
}
