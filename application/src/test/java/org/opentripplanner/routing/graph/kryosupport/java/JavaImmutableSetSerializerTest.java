package org.opentripplanner.routing.graph.kryosupport.java;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JavaImmutableSetSerializerTest {

  /** Covers the empty, the 1 and 2 element, and the N element implementations of Set.of */
  static Stream<Set<Integer>> sets() {
    return Stream.of(Set.of(), Set.of(1), Set.of(1, 2), Set.of(3, 1, 2), Set.of(1, 2, 3, 4));
  }

  @ParameterizedTest
  @MethodSource("sets")
  void roundTrip(Set<Integer> set) {
    assertThat(writeAndRead(set)).containsExactlyElementsIn(set);
  }

  @ParameterizedTest
  @MethodSource("sets")
  void readSetIsImmutable(Set<Integer> set) {
    var read = writeAndRead(set);
    assertThrows(UnsupportedOperationException.class, () -> read.add(9));
  }

  @Test
  void toUnmodifiableSet() {
    var set = Stream.of("a", "b", "c").collect(Collectors.toUnmodifiableSet());
    assertThat(writeAndRead(set)).containsExactly("a", "b", "c");
  }

  @Test
  void nestedSets() {
    var value = Set.of(Set.of(1, 2), Set.of(), Set.of(3));
    assertThat(writeAndRead(value)).isEqualTo(value);
  }
}
