package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyGraphQLTripImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLTrip {

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> new Relay.ResolvedGlobalId("Trip",
        getSource(environment).getId().toString());
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<Route> route() {
    return environment -> getSource(environment).getRoute();
  }

  @Override
  public DataFetcher<String> serviceId() {
    return environment -> getSource(environment).getServiceId().toString();
  }

  @Override
  public DataFetcher<Iterable<String>> activeDates() {
    return environment -> getRoutingService(environment).getCalendarService()
        .getServiceDatesForServiceId(getSource(environment).getServiceId())
        .stream()
        .map(ServiceDate::asCompactString)
        .collect(Collectors.toList());
  }

  @Override
  public DataFetcher<String> tripShortName() {
    return environment -> getSource(environment).getTripShortName();
  }

  @Override
  public DataFetcher<String> tripHeadsign() {
    return environment -> getSource(environment).getTripHeadsign();
  }

  @Override
  public DataFetcher<String> routeShortName() {
    return environment -> {
      Trip trip = getSource(environment);

      return trip.getRouteShortName() != null ? trip.getRouteShortName() : trip.getRoute().getShortName();
    };
  }

  @Override
  public DataFetcher<String> directionId() {
    return environment -> getSource(environment).getGtfsDirectionIdAsString(null);
  }

  @Override
  public DataFetcher<String> blockId() {
    return environment -> getSource(environment).getBlockId();
  }

  @Override
  public DataFetcher<String> shapeId() {
    return environment -> getSource(environment).getShapeId().toString();
  }

  @Override
  public DataFetcher<Object> wheelchairAccessible() {
    return environment -> {
      switch (getSource(environment).getWheelchairAccessible()) {
        case 0: return "NO_INFORMATION";
        case 1: return "POSSIBLE";
        case 2: return "NOT_POSSIBLE";
        default: return null;
      }
    };
  }

  @Override
  public DataFetcher<String> bikesAllowed() {
    return environment -> {
      switch (getSource(environment).getBikesAllowed()) {
        case UNKNOWN: return "NO_INFORMATION";
        case ALLOWED: return "POSSIBLE";
        case NOT_ALLOWED: return "NOT_POSSIBLE";
        default: return null;
      }
    };
  }

  @Override
  public DataFetcher<TripPattern> pattern() {
    return this::getTripPattern;
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return environment -> {
      TripPattern tripPattern = getTripPattern(environment);
      if (tripPattern == null) { return List.of(); }
      return List.copyOf(tripPattern.getStops());
    };
  }

  @Override
  public DataFetcher<String> semanticHash() {
    return environment -> {
      TripPattern tripPattern = getTripPattern(environment);
      if (tripPattern == null) { return null; }
      return tripPattern.semanticHashString(getSource(environment));
    };
  }

  @Override
  public DataFetcher<Iterable<TripTimeShort>> stoptimes() {
    return environment -> {
      TripPattern tripPattern = getTripPattern(environment);
      if (tripPattern == null) { return List.of(); }
      return TripTimeShort.fromTripTimes(tripPattern.scheduledTimetable, getSource(environment));
    };
  }

  @Override
  public DataFetcher<TripTimeShort> departureStoptime() {
    return environment -> {
      try {
        RoutingService routingService = getRoutingService(environment);
        TripPattern tripPattern = getTripPattern(environment);
        if (tripPattern == null) { return null; }
        Timetable timetable = tripPattern.scheduledTimetable;

        TripTimes triptimes = timetable.getTripTimes(getSource(environment));
        ServiceDay serviceDate = null;

        var args = new LegacyGraphQLTypes.LegacyGraphQLTripDepartureStoptimeArgs(environment.getArguments());
        if (args.getLegacyGraphQLServiceDate() != null)
        new ServiceDay(
            routingService.getServiceCodes(),
            ServiceDate.parseString(args.getLegacyGraphQLServiceDate()),
            routingService.getCalendarService(),
            getSource(environment).getRoute().getAgency().getId()
        );

        Stop stop = timetable.pattern.getStop(0);
        return new TripTimeShort(triptimes, 0, stop, serviceDate
        );
      } catch (ParseException e) {
        //Invalid date format
        return null;
      }
    };
  }

  @Override
  public DataFetcher<TripTimeShort> arrivalStoptime() {
    return environment -> {
      try {
        RoutingService routingService = getRoutingService(environment);
        TripPattern tripPattern = getTripPattern(environment);
        if (tripPattern == null) { return null; }
        Timetable timetable = tripPattern.scheduledTimetable;

        TripTimes triptimes = timetable.getTripTimes(getSource(environment));
        ServiceDay serviceDate = null;

        var args = new LegacyGraphQLTypes.LegacyGraphQLTripArrivalStoptimeArgs(environment.getArguments());
        if (args.getLegacyGraphQLServiceDate() != null)
          new ServiceDay(
              routingService.getServiceCodes(),
              ServiceDate.parseString(args.getLegacyGraphQLServiceDate()),
              routingService.getCalendarService(),
              getSource(environment).getRoute().getAgency().getId()
          );

        Stop stop = timetable.pattern.getStop(triptimes.getNumStops() - 1);
        return new TripTimeShort(triptimes, triptimes.getNumStops() - 1, stop, serviceDate
        );
      } catch (ParseException e) {
        //Invalid date format
        return null;
      }
    };
  }

  @Override
  public DataFetcher<Iterable<TripTimeShort>> stoptimesForDate() {
    return environment -> {
      try {
        RoutingService routingService = getRoutingService(environment);
        Trip trip = getSource(environment);
        TripPattern tripPattern = getTripPattern(environment);
        if (tripPattern == null) { return List.of(); }

        var args = new LegacyGraphQLTypes.LegacyGraphQLTripStoptimesForDateArgs(environment.getArguments());


        ServiceDate serviceDate = args.getLegacyGraphQLServiceDate() != null
            ? ServiceDate.parseString(args.getLegacyGraphQLServiceDate()) : new ServiceDate();

        ServiceDay serviceDay = new ServiceDay(
            routingService.getServiceCodes(),
            serviceDate,
            routingService.getCalendarService(),
            trip.getRoute().getAgency().getId()
        );

        //TODO: Pass serviceDate
        Timetable timetable = routingService.getTimetableForTripPattern(tripPattern);
        return TripTimeShort.fromTripTimes(timetable, trip, serviceDay);
      } catch (ParseException e) {
        return null; // Invalid date format
      }
    };
  }

  @Override
  public DataFetcher<Iterable<Iterable<Double>>> geometry() {
    return environment -> {
      TripPattern tripPattern = getTripPattern(environment);
      if (tripPattern == null) { return null; }

      LineString geometry = tripPattern.getGeometry();
      if (geometry == null) {
        return null;
      }
      return Arrays.stream(geometry.getCoordinateSequence().toCoordinateArray())
          .map(coordinate -> Arrays.asList(coordinate.x, coordinate.y))
          .collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<EncodedPolylineBean> tripGeometry() {
    return environment -> {
      TripPattern tripPattern = getTripPattern(environment);
      if (tripPattern == null) { return null; }
      LineString geometry = tripPattern.getGeometry();
      if (geometry == null) { return null; }
      return PolylineEncoder.createEncodings(Arrays.asList(geometry.getCoordinates()));
    };
  }

  //TODO
  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> List.of();
  }

  private TripPattern getTripPattern(DataFetchingEnvironment environment) {
    return getRoutingService(environment).getPatternForTrip().get(environment.getSource());
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private Trip getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
