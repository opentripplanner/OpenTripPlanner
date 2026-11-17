package org.opentripplanner.routing.graph.kryosupport;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnumMapSerializerTest {

  @Test
  void roundTrip() {
    var map = new EnumMap<DayOfWeek, String>(DayOfWeek.class);
    map.put(DayOfWeek.MONDAY, "mon");
    map.put(DayOfWeek.FRIDAY, "fri");

    var read = writeAndRead(map);

    assertThat(read).isInstanceOf(EnumMap.class);
    assertThat(read).containsExactly(DayOfWeek.MONDAY, "mon", DayOfWeek.FRIDAY, "fri").inOrder();
  }

  @Test
  void singleEntry() {
    var map = new EnumMap<>(Map.of(DayOfWeek.SUNDAY, 7));
    assertThat(writeAndRead(map)).containsExactly(DayOfWeek.SUNDAY, 7);
  }

  @Test
  void enumMapCanBeModifiedAfterRead() {
    var read = writeAndRead(new EnumMap<>(Map.of(DayOfWeek.MONDAY, 1)));
    read.put(DayOfWeek.TUESDAY, 2);
    assertThat(read).hasSize(2);
  }

  @Test
  void enumMapInsideImmutableMap() {
    var value = Map.of("k", new EnumMap<>(Map.of(DayOfWeek.MONDAY, "mon")));
    assertThat(writeAndRead(value)).isEqualTo(value);
  }
}
