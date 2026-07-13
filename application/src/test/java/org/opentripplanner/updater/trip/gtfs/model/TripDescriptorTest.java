package org.opentripplanner.updater.trip.gtfs.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.transit.realtime.GtfsRealtime;
import java.time.LocalTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.updater.spi.UpdateException;

class TripDescriptorTest {

  @ParameterizedTest
  @ValueSource(strings = { "12:00:00", "12:00" })
  void startTime(String time) {
    var raw = GtfsRealtime.TripDescriptor.newBuilder().setStartTime(time).build();
    var descriptor = new TripDescriptor(raw);

    assertThat(descriptor.startTime()).hasValue(LocalTime.of(12, 0));
  }

  @ParameterizedTest
  @ValueSource(strings = { "12", "AAA", "1200" })
  void exception(String time) {
    var raw = GtfsRealtime.TripDescriptor.newBuilder().setStartTime(time).build();
    var descriptor = new TripDescriptor(raw);

    assertThrows(UpdateException.class, descriptor::startTime);
  }
}
