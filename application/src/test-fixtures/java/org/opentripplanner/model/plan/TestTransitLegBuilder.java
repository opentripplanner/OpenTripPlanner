package org.opentripplanner.model.plan;

import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.TestStopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

public class TestTransitLegBuilder {

  private static final Agency AGENCY = Agency.of(id("a1"))
    .withName("Test Agency")
    .withTimezone(ZoneIds.UTC.toString())
    .build();
  private static final Route ROUTE = Route.of(id("r1"))
    .withShortName("Test Route")
    .withAgency(AGENCY)
    .withMode(TransitMode.BUS)
    .build();
  private static final LocalDate DATE = LocalDate.parse("2025-11-17");
  private static final ZonedDateTime TIME = LocalTime.parse("12:00")
    .atDate(DATE)
    .atZone(ZoneIds.UTC);
  ZonedDateTime startTime = TIME;
  ZonedDateTime endTime = TIME.plusHours(1);
  Trip trip = Trip.of(id("t1")).withRoute(ROUTE).withServiceId(id("s1")).build();
  StopLocation from = new TestStopLocation(id("s1"));
  StopLocation to = new TestStopLocation(id("s2"));

  public TestTransitLegBuilder withStartTime(String startTime) {
    var time = LocalTime.parse(startTime);
    this.startTime = DATE.atTime(time).atZone(ZoneIds.UTC);
    return this;
  }

  public TestTransitLegBuilder withEndTime(String endTime) {
    var time = LocalTime.parse(endTime);
    this.endTime = DATE.atTime(time).atZone(ZoneIds.UTC);
    return this;
  }

  public TestTransitLegBuilder withServiceId(FeedScopedId serviceId) {
    this.trip = trip.copy().withServiceId(serviceId).build();
    return this;
  }

  public TestTransitLegBuilder withFrom(FeedScopedId id) {
    this.from = new TestStopLocation(id);
    return this;
  }

  public TestTransitLegBuilder withTo(FeedScopedId id) {
    this.to = new TestStopLocation(id);
    return this;
  }

  public TestTransitLegBuilder withRoute(Route route) {
    this.trip = this.trip.copy().withRoute(route).build();
    return this;
  }

  public TestTransitLeg build() {
    return new TestTransitLeg(this);
  }
}
