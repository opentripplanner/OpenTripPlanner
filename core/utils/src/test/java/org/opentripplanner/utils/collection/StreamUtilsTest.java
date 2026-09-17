package org.opentripplanner.utils.collection;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StreamUtilsTest {

  @Test
  void nullableCollection() {
    assertEquals(List.of(), StreamUtils.ofNullableCollection(null).toList());
  }

  @Test
  void nullableCollectionEmptyList() {
    assertEquals(List.of(), StreamUtils.ofNullableCollection(List.of()).toList());
  }

  @Test
  void nullableCollectionOneElement() {
    assertEquals(List.of(1), StreamUtils.ofNullableCollection(List.of(1)).toList());
  }

  @Test
  void nullableCollectionTwoElements() {
    assertEquals(List.of(1, 2), StreamUtils.ofNullableCollection(List.of(1, 2)).toList());
  }

  @Test
  void ofIterableEmpty() {
    assertThat(StreamUtils.ofIterable(List.of()).toList()).isEmpty();
  }

  @Test
  void ofIterableList() {
    assertThat(StreamUtils.ofIterable(List.of(1, 2, 3)).toList())
      .containsExactly(1, 2, 3)
      .inOrder();
  }

  @Test
  void ofIterableNonCollectionIterable() {
    Iterable<Integer> iterable = Set.of(1, 2, 3)::iterator;
    assertThat(StreamUtils.ofIterable(iterable).toList()).containsExactly(1, 2, 3);
  }
}
