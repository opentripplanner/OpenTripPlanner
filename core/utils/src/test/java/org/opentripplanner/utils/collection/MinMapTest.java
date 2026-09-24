package org.opentripplanner.utils.collection;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Comparator;
import org.junit.jupiter.api.Test;

class MinMapTest {

  private static final String KEY = "key";
  private final MinMap<String, String> subject = MinMap.ofNaturalOrder();

  @Test
  void putMinAndGet() {
    assertNull(subject.get(KEY));

    subject.putMin(KEY, "orange");
    assertEquals("orange", subject.get(KEY));

    subject.putMin(KEY, "apple");
    assertEquals("apple", subject.get(KEY));

    subject.putMin(KEY, "banana");
    assertEquals("apple", subject.get(KEY));
  }

  @Test
  void values() {
    subject.putMin("key1", "orange");
    assertThat(subject.values()).containsExactly("orange");

    subject.putMin("key2", "apple");
    assertThat(subject.values()).containsExactly("apple", "orange");

    subject.putMin("key3", "banana");
    assertThat(subject.values()).containsExactly("apple", "banana", "orange");
  }

  @Test
  void customOrder() {
    var subject = new MinMap<String, String>(Comparator.<String>naturalOrder().reversed());
    subject.putMin(KEY, "A");
    assertEquals("A", subject.get(KEY));

    subject.putMin(KEY, "B");
    assertEquals("B", subject.get(KEY));

    subject.putMin(KEY, "X");
    assertEquals("X", subject.get(KEY));

    subject.putMin(KEY, "A");
    assertEquals("X", subject.get(KEY));
  }
}
