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
import org.opentripplanner.transit.model._data.TransitModelForTest;

class StreetSearchRequestMapperTest {

  @Test
  void map() {
    RouteRequest routeRequest = new RouteRequest();

    Instant dateTime = Instant.parse("2022-11-10T10:00:00Z");
    routeRequest.setDateTime(dateTime);
    var from = new GenericLocation(null, TransitModelForTest.id("STOP"), null, null);
    routeRequest.setFrom(from);
    var to = new GenericLocation(60.0, 20.0);
    routeRequest.setTo(to);
    routeRequest.withPreferences(it -> it.withWalk(walk -> walk.withSpeed(2.4)));
    routeRequest.setWheelchair(true);
    var modes = RequestModes.of().withAllStreetModes(StreetMode.BIKE).build();
    routeRequest.journey().setModes(modes);

    var subject = StreetSearchRequestMapper.map(routeRequest).build();

    assertEquals(dateTime, subject.startTime());
    assertEquals(from, subject.from());
    assertEquals(to, subject.to());
    assertEquals(routeRequest.preferences(), subject.preferences());
    assertTrue(subject.wheelchair());
  }

  @Test
  void mapToTransferRequest() {
    RouteRequest routeRequest = new RouteRequest();

    Instant dateTime = Instant.parse("2022-11-10T10:00:00Z");
    routeRequest.setDateTime(dateTime);
    var from = new GenericLocation(null, TransitModelForTest.id("STOP"), null, null);
    routeRequest.setFrom(from);
    var to = new GenericLocation(60.0, 20.0);
    routeRequest.setTo(to);
    routeRequest.withPreferences(it -> it.withWalk(walk -> walk.withSpeed(2.4)));
    routeRequest.setWheelchair(true);

    var subject = StreetSearchRequestMapper.mapToTransferRequest(routeRequest).build();

    assertEquals(Instant.EPOCH, subject.startTime());
    assertNull(subject.from());
    assertNull(subject.to());
    assertEquals(routeRequest.preferences(), subject.preferences());
    assertTrue(subject.wheelchair());
  }
}
