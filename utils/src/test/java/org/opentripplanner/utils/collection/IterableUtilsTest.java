package org.opentripplanner.utils.collection;

import static com.google.common.truth.Truth.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class IterableUtilsTest {

  @Test
  void of() {
    var iterable = IterableUtils.of(Stream.of("A", "C", "B"));
    assertThat(iterable).containsExactly("A", "C", "B").inOrder();
  }
}
