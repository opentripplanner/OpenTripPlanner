package org.opentripplanner.transit.model._data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.function.Consumer;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RouteBuilder;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.organization.OperatorBuilder;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.RegularStopBuilder;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StationBuilder;
import org.opentripplanner.transit.model.timetable.TripBuilder;
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
    return new TransitTestEnvironment(
      timetableRepository,
      TransferServiceTestFactory.defaultTransferRepository(),
      defaultServiceDate
    );
  }

  /**
   * The default service date is used when creating trips without an explicit service date
   */
  public LocalDate defaultServiceDate() {
    return defaultServiceDate;
  }

  /* SITE ENTITIES */

  public RegularStop stop(String id) {
    return site.stop(id);
  }

  public RegularStop stop(String id, Consumer<RegularStopBuilder> customizer) {
    return site.stop(id, customizer);
  }

  public TransitTestEnvironmentBuilder addStops(String... stopIds) {
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

  public Station station(String id) {
    return site.station(id);
  }

  public Station station(String id, Consumer<StationBuilder> customizer) {
    return site.station(id, customizer);
  }

  /* TIMETABLE ENTITIES */

  public TimetableRepositoryTestBuilder timetable() {
    return timetable;
  }

  public Route route(String routeId) {
    return timetable.route(routeId);
  }

  public Route route(String routeId, Operator operator) {
    return timetable.route(routeId, operator);
  }

  public Route route(String routeId, Consumer<RouteBuilder> customizer) {
    return timetable.route(routeId, customizer);
  }

  public Operator operator(String operatorId) {
    return timetable.operator(operatorId);
  }

  public Operator operator(String operatorId, Consumer<OperatorBuilder> customizer) {
    return timetable.operator(operatorId, customizer);
  }

  public TransitTestEnvironmentBuilder addTrip(TripInput trip) {
    timetable.trip(trip);
    return this;
  }

  public TransitTestEnvironmentBuilder addTrip(TripInput trip, Consumer<TripBuilder> consumer) {
    timetable.trip(trip, consumer);
    return this;
  }

  public TransitTestEnvironmentBuilder addScheduledStopPointMapping(
    FeedScopedId scheduledStopPointId,
    RegularStop stop
  ) {
    timetable().addScheduledStopPointMapping(scheduledStopPointId, stop);
    return this;
  }
}
