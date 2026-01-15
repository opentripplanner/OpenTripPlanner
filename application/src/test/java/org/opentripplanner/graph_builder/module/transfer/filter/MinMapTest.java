package org.opentripplanner.graph_builder.module.transfer.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.common.truth.Truth;
import org.junit.jupiter.api.Test;

class MinMapTest {

  private static final String KEY = "key";
  private final MinMap<String, String> subject = new MinMap<>();

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
    Truth.assertThat(subject.values()).containsExactly("orange");

    subject.putMin("key2", "apple");
    Truth.assertThat(subject.values()).containsExactly("apple", "orange");

    subject.putMin("key3", "banana");
    Truth.assertThat(subject.values()).containsExactly("apple", "banana", "orange");
  }
}
