package org.opentripplanner.utils.collection;

import static com.google.common.truth.Truth.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class IterableUtilsTest {

  @Test
  void ofStream() {
    var iterable = IterableUtils.ofStream(Stream.of("A", "C", "B"));
    assertThat(iterable).containsExactly("A", "C", "B").inOrder();
  }
}
