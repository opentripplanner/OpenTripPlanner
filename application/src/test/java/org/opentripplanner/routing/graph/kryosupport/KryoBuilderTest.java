package org.opentripplanner.routing.graph.kryosupport;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests the library serializers registered in {@link KryoBuilder}. The custom serializers have
 * their own tests.
 */
class KryoBuilderTest {

  @Test
  void hashMultimap() {
    var map = HashMultimap.<String, Integer>create();
    map.put("a", 1);
    map.put("a", 2);
    map.put("b", 3);

    var copy = writeAndRead(map);

    assertThat(copy.get("a")).containsExactly(1, 2);
    assertThat(copy.get("b")).containsExactly(3);
  }

  @Test
  void arrayListMultimap() {
    var map = ArrayListMultimap.<String, Integer>create();
    map.put("a", 2);
    map.put("a", 1);

    assertThat(writeAndRead(map).get("a")).containsExactly(2, 1).inOrder();
  }

  /** Immutable collections nested in each other, as they are in the graph. */
  @Test
  void nestedImmutableCollections() {
    var value = Map.of("k", List.of(Set.of(1)));
    assertThat(writeAndRead(value)).isEqualTo(value);
  }
}
