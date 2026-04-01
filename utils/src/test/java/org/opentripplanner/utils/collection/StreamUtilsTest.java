package org.opentripplanner.utils.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
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
}
