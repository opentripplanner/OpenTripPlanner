package org.opentripplanner.routing.graph.kryosupport;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JavaImmutableMapSerializerTest {

  /** Covers the empty, the 1 entry, and the N entry implementations of Map.of */
  static Stream<Map<String, Integer>> maps() {
    return Stream.of(
      Map.of(),
      Map.of("a", 1),
      Map.of("a", 1, "b", 2),
      Map.of("a", 1, "b", 2, "c", 3),
      Map.of("a", 1, "b", 2, "c", 3, "d", 4)
    );
  }

  @ParameterizedTest
  @MethodSource("maps")
  void roundTrip(Map<String, Integer> map) {
    assertThat(writeAndRead(map)).containsExactlyEntriesIn(map);
  }

  @ParameterizedTest
  @MethodSource("maps")
  void readMapIsImmutable(Map<String, Integer> map) {
    var read = writeAndRead(map);
    assertThrows(UnsupportedOperationException.class, () -> read.put("z", 9));
  }

  @Test
  void toUnmodifiableMap() {
    var map = Stream.of("a", "bb").collect(Collectors.toUnmodifiableMap(s -> s, String::length));
    assertThat(writeAndRead(map)).containsExactly("a", 1, "bb", 2);
  }

  @Test
  void mapWithCollectionValues() {
    var value = Map.of("k", List.of(1, 2), "l", List.of());
    assertThat(writeAndRead(value)).isEqualTo(value);
  }
}
