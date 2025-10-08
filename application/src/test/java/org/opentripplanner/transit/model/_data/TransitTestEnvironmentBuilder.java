package org.opentripplanner.transit.model._data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;

public class TransitTestEnvironmentBuilder {

  private final SiteRepositoryTestBuilder site;
  private final TimetableRepositoryTestBuilder timetable;

  private final LocalDate defaultServiceDate;

  TransitTestEnvironmentBuilder(ZoneId timeZone, LocalDate defaultServiceDate) {
    this.defaultServiceDate = defaultServiceDate;

    site = new SiteRepositoryTestBuilder(SiteRepository.of());
    timetable = new TimetableRepositoryTestBuilder(timeZone, defaultServiceDate);
  }

  public TransitTestEnvironment build() {
    var siteRepository = site.build();
    var timetableRepository = timetable.build(siteRepository);
    return new TransitTestEnvironment(timetableRepository, defaultServiceDate);
  }

  /**
   * The default service date is used when creating trips without a specified service date
   */
  public LocalDate defaultServiceDate() {
    return defaultServiceDate;
  }

  /* ~~~~~~~~~~~~~ *
   * Site entities *
   * ~~~~~~~~~~~~~ */

  public RegularStop stop(String id) {
    return site.stop(id);
  }

  public TransitTestEnvironmentBuilder withStops(String... stopIds) {
    Arrays.stream(stopIds).forEach(site::stop);
    return this;
  }

  /**
   * Add a stop at a station.  The station will be created if it does not already exist.
   */
  public RegularStop stopAtStation(String stopId, String stationId) {
    return site.stopAtStation(stopId, stationId);
  }

  public AreaStop areaStop(String id) {
    return site.areaStop(id);
  }

  /* ~~~~~~~~~~~~~~~~~~ *
   * Timetable entities *
   * ~~~~~~~~~~~~~~~~~~ */

  public TimetableRepositoryTestBuilder timetable() {
    return timetable;
  }

  public Route route(String routeId) {
    return timetable.route(routeId);
  }

  public Route route(String routeId, Operator operator) {
    return timetable.route(routeId, operator);
  }

  public Operator operator(String operatorId) {
    return timetable.operator(operatorId);
  }

  public TransitTestEnvironmentBuilder withTrip(TripInput trip) {
    timetable.withTrip(trip);
    return this;
  }

  public TransitTestEnvironmentBuilder withFlexTrip(FlexTripInput tripInput) {
    timetable.withFlexTrip(tripInput);
    return this;
  }

  public TransitTestEnvironmentBuilder withScheduledStopPointMapping(
    FeedScopedId scheduledStopPointId,
    RegularStop stop
  ) {
    timetable().withScheduledStopPointMapping(scheduledStopPointId, stop);
    return this;
  }
}
