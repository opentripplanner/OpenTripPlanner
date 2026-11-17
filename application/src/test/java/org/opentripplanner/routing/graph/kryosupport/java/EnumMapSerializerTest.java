package org.opentripplanner.routing.graph.kryosupport;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
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
  void emptyMapKeepsItsEnumType() {
    var read = writeAndRead(new EnumMap<DayOfWeek, String>(DayOfWeek.class));

    assertThat(read).isEmpty();
    read.put(DayOfWeek.MONDAY, "mon");
    assertThat(read).containsExactly(DayOfWeek.MONDAY, "mon");
  }

  @Test
  @SuppressWarnings("unchecked")
  void emptyMapsOfDifferentEnumTypes() {
    var days = new EnumMap<DayOfWeek, String>(DayOfWeek.class);
    var units = new EnumMap<ChronoUnit, String>(ChronoUnit.class);

    List<Object> read = writeAndRead(List.<Object>of(days, units));

    var readDays = (EnumMap<DayOfWeek, String>) read.get(0);
    var readUnits = (EnumMap<ChronoUnit, String>) read.get(1);
    assertThat(readDays).isEmpty();
    assertThat(readUnits).isEmpty();
    readDays.put(DayOfWeek.FRIDAY, "fri");
    readUnits.put(ChronoUnit.DAYS, "days");
  }

  /** The Java serialization of the empty map must not consume the data written after it. */
  @Test
  void dataAfterEmptyMapIsNotConsumed() {
    var empty = new EnumMap<DayOfWeek, String>(DayOfWeek.class);
    var nonEmpty = new EnumMap<>(Map.of(DayOfWeek.MONDAY, "mon"));

    List<Object> read = writeAndRead(List.<Object>of(empty, "after", nonEmpty, "end"));

    assertThat(read.get(0)).isEqualTo(empty);
    assertThat(read.get(1)).isEqualTo("after");
    assertThat(read.get(2)).isEqualTo(nonEmpty);
    assertThat(read.get(3)).isEqualTo("end");
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
