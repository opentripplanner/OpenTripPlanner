package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.text.ParseException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyGraphQLPatternImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLPattern {

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> new Relay.ResolvedGlobalId("Pattern",
        getSource(environment).getId().toString()
    );
  }

  @Override
  public DataFetcher<Route> route() {
    return environment -> getSource(environment).route;
  }

  @Override
  public DataFetcher<Integer> directionId() {
    return environment -> getSource(environment).getDirection().gtfsCode;
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> getSource(environment).name;
  }

  @Override
  public DataFetcher<String> code() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<String> headsign() {
    return environment -> getSource(environment).getTripHeadsign();
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return environment -> getSource(environment).getTrips();
  }

  @Override
  public DataFetcher<Iterable<Trip>> tripsForDate() {
    return environment -> {
      String servicaDate = new LegacyGraphQLTypes.LegacyGraphQLPatternTripsForDateArgs(environment.getArguments()).getLegacyGraphQLServiceDate();

      try {
        BitSet services = getRoutingService(environment).getServicesRunningForDate(
            ServiceDate.parseString(servicaDate)
        );
        return getSource(environment).scheduledTimetable.tripTimes
            .stream()
            .filter(times -> services.get(times.serviceCode))
            .map(times -> times.trip)
            .collect(Collectors.toList());
      } catch (ParseException e) {
        return null; // Invalid date format
      }
    };
  }

  @Override
  public DataFetcher<Iterable<Object>> stops() {
    return environment -> getSource(environment)
        .getStops()
        .stream()
        .map(Object.class::cast)
        .collect(Collectors.toList());
  }

  @Override
  public DataFetcher<Iterable<Coordinate>> geometry() {
    return environment -> {
      LineString geometry = getSource(environment).getGeometry();
      if (geometry == null) {
        return null;
      } else {
        return Arrays.asList(geometry.getCoordinates());
      }
    };
  }

  @Override
  public DataFetcher<EncodedPolylineBean> patternGeometry() {
    return environment -> {
      LineString geometry = getSource(environment).getGeometry();
      if (geometry == null) {
        return null;
      }

      return PolylineEncoder.createEncodings(Arrays.asList(geometry.getCoordinates()));
    };
  }

  @Override
  public DataFetcher<String> semanticHash() {
    return environment -> getSource(environment).semanticHashString(null);
  }

  // TODO
  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> List.of();
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private TripPattern getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
