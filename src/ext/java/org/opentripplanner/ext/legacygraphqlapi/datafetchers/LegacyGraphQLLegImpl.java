package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LegacyGraphQLLegImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLLeg {

  @Override
  public DataFetcher<Long> startTime() {
    return environment -> getSource(environment).startTime.getTime().getTime();
  }

  @Override
  public DataFetcher<Long> endTime() {
    return environment -> getSource(environment).endTime.getTime().getTime();
  }

  @Override
  public DataFetcher<Integer> departureDelay() {
    return environment -> getSource(environment).departureDelay;
  }

  @Override
  public DataFetcher<Integer> arrivalDelay() {
    return environment -> getSource(environment).arrivalDelay;
  }

  @Override
  public DataFetcher<LegacyGraphQLTypes.LegacyGraphQLMode> mode() {
    return environment -> LegacyGraphQLTypes.LegacyGraphQLMode.valueOfLabel(getSource(environment).mode.name());
  }

  @Override
  public DataFetcher<Double> duration() {
    return environment -> (double) getSource(environment).getDuration();
  }

  @Override
  public DataFetcher<EncodedPolylineBean> legGeometry() {
    return environment -> getSource(environment).legGeometry;
  }

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> getRoutingService(environment).getAgencyForId(getSource(environment).agencyId);
  }

  @Override
  public DataFetcher<Boolean> realTime() {
    return environment -> getSource(environment).realTime;
  }

  // TODO
  @Override
  public DataFetcher<LegacyGraphQLTypes.LegacyGraphQLRealtimeState> realtimeState() {
    return environment -> null;
  }

  @Override
  public DataFetcher<Double> distance() {
    return environment -> getSource(environment).distanceMeters;
  }

  @Override
  public DataFetcher<Boolean> transitLeg() {
    return environment -> getSource(environment).isTransitLeg();
  }

  @Override
  public DataFetcher<Boolean> rentedBike() {
    return environment -> getSource(environment).rentedBike;
  }

  @Override
  public DataFetcher<Place> from() {
    return environment -> getSource(environment).from;
  }

  @Override
  public DataFetcher<Place> to() {
    return environment -> getSource(environment).to;
  }

  @Override
  public DataFetcher<Route> route() {
    return environment -> getRoutingService(environment).getRouteForId(getSource(environment).routeId);
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getRoutingService(environment).getTripForId().get(getSource(environment).tripId);

  }

  @Override
  public DataFetcher<String> serviceDate() {
    return environment -> getSource(environment).serviceDate;
  }

  @Override
  public DataFetcher<Iterable<Object>> intermediateStops() {
    return environment -> {
      List<StopArrival> intermediateStops = getSource(environment).intermediateStops;
      if (intermediateStops == null) return null;
      RoutingService routingService = getRoutingService(environment);
      return intermediateStops.stream()
          .map(intermediateStop -> intermediateStop.place)
          .filter(place -> place.stopId != null)
          .map(place -> routingService.getStopForId(place.stopId))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Iterable<Place>> intermediatePlaces() {
    return environment -> {
      List<StopArrival> intermediateStops = getSource(environment).intermediateStops;
      if (intermediateStops == null) return null;
      return intermediateStops.stream().map(stopArrival -> stopArrival.place).collect(Collectors.toList());
    };
  }

  // TODO
  @Override
  public DataFetcher<Boolean> intermediatePlace() {
    return environment -> false;
  }

  @Override
  public DataFetcher<Iterable<WalkStep>> steps() {
    return environment -> getSource(environment).walkSteps;
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private Leg getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
