package org.opentripplanner.routing.graph.kryosupport;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JavaImmutableListSerializerTest {

  /** Covers the empty, the 1 and 2 element, and the N element implementations of List.of */
  static Stream<List<Integer>> lists() {
    return Stream.of(List.of(), List.of(1), List.of(1, 2), List.of(3, 1, 2), List.of(1, 2, 3, 4));
  }

  @ParameterizedTest
  @MethodSource("lists")
  void roundTrip(List<Integer> list) {
    assertThat(writeAndRead(list)).containsExactlyElementsIn(list).inOrder();
  }

  @ParameterizedTest
  @MethodSource("lists")
  void readListIsImmutable(List<Integer> list) {
    var read = writeAndRead(list);
    assertThrows(UnsupportedOperationException.class, () -> read.add(9));
  }

  @Test
  void toUnmodifiableList() {
    var list = Stream.of("a", "b", "c").collect(Collectors.toUnmodifiableList());
    assertThat(writeAndRead(list)).containsExactly("a", "b", "c").inOrder();
  }

  @Test
  void nestedLists() {
    var value = List.of(List.of(1, 2), List.of(), List.of(3));
    assertThat(writeAndRead(value)).isEqualTo(value);
  }
}
