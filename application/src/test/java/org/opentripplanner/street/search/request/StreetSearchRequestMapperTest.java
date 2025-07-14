package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class StreetSearchRequestMapperTest {

  @Test
  void map() {
    var builder = RouteRequest.of();

    Instant dateTime = Instant.parse("2022-11-10T10:00:00Z");
    builder.withDateTime(dateTime);
    var from = new GenericLocation(null, TimetableRepositoryForTest.id("STOP"), null, null);
    builder.withFrom(from);
    var to = GenericLocation.fromCoordinate(60.0, 20.0);
    builder.withTo(to);
    builder.withPreferences(it -> it.withWalk(walk -> walk.withSpeed(2.4)));

    builder.withJourney(jb ->
      jb
        .withWheelchair(true)
        .setModes(RequestModes.of().withAllStreetModes(StreetMode.BIKE).build())
    );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.map(request).build();

    assertEquals(dateTime, subject.startTime());
    assertEquals(from, subject.from());
    assertEquals(to, subject.to());
    assertEquals(request.preferences(), subject.preferences());
    assertTrue(subject.wheelchair());
  }

  @Test
  void mapToTransferRequest() {
    var builder = RouteRequest.of();

    Instant dateTime = Instant.parse("2022-11-10T10:00:00Z");
    builder.withDateTime(dateTime);
    var from = new GenericLocation(null, TimetableRepositoryForTest.id("STOP"), null, null);
    builder.withFrom(from);
    var to = GenericLocation.fromCoordinate(60.0, 20.0);
    builder.withTo(to);
    builder.withPreferences(it -> it.withWalk(walk -> walk.withSpeed(2.4)));
    builder.withJourney(j -> j.withWheelchair(true));

    var request = builder.buildRequest();

    var subject = StreetSearchRequestMapper.mapToTransferRequest(request).build();

    assertEquals(Instant.EPOCH, subject.startTime());
    assertNull(subject.from());
    assertNull(subject.to());
    assertEquals(request.preferences(), subject.preferences());
    assertTrue(subject.wheelchair());
  }
}
