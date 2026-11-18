package org.opentripplanner.routing.graph.kryosupport.hppc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.write;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the HPPC types without a serializer. The supported types are tested by their own serializer
 * test, like {@link IntHashSetSerializerTest}.
 */
class HppcKryoSupportTest {

  static Stream<Object> unsupportedTypes() {
    return Stream.of(
      new IntArrayList(),
      new LongHashSet(),
      new LongObjectHashMap<String>(),
      new ObjectIntHashMap<String>()
    );
  }

  @ParameterizedTest
  @MethodSource("unsupportedTypes")
  void unsupportedTypeIsRejectedOnWrite(Object container) {
    var ex = assertThrows(RuntimeException.class, () -> write(container));
    assertThat(rootCause(ex)).isInstanceOf(UnsupportedOperationException.class);
    assertThat(rootCause(ex)).hasMessageThat().contains(container.getClass().getName());
  }

  @Test
  void unsupportedTypeNestedInOtherObjectIsRejected() {
    assertThrows(RuntimeException.class, () -> write(List.of(new LongHashSet())));
  }

  private static Throwable rootCause(Throwable t) {
    while (t.getCause() != null) {
      t = t.getCause();
    }

    return t;
  }
}
