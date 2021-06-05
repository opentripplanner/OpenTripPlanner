package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.api.mapping.ServiceDateMapper;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.plan.Leg;
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
  public DataFetcher<String> mode() {
    return environment -> getSource(environment).mode.name();
  }

  @Override
  public DataFetcher<Double> duration() {
    return environment -> (double) getSource(environment).getDuration();
  }

  @Override
  public DataFetcher<Integer> generalizedCost() {
    return environment -> getSource(environment).generalizedCost;
  }

  @Override
  public DataFetcher<EncodedPolylineBean> legGeometry() {
    return environment -> getSource(environment).legGeometry;
  }

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> getSource(environment).getAgency();
  }

  @Override
  public DataFetcher<Boolean> realTime() {
    return environment -> getSource(environment).realTime;
  }

  // TODO
  @Override
  public DataFetcher<String> realtimeState() {
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
  public DataFetcher<StopArrival> from() {
    return environment -> {
      Leg source = getSource(environment);
      return new StopArrival(source.from, source.startTime, source.startTime);
    };
  }

  @Override
  public DataFetcher<StopArrival> to() {
    return environment -> {
      Leg source = getSource(environment);
      return new StopArrival(source.to, source.endTime, source.endTime);
    };
  }

  @Override
  public DataFetcher<Route> route() {
    return environment -> getSource(environment).getRoute();
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getSource(environment).getTrip();
  }

  @Override
  public DataFetcher<String> serviceDate() {
    return environment -> ServiceDateMapper.mapToApi(getSource(environment).serviceDate);
  }

  @Override
  public DataFetcher<Iterable<Object>> intermediateStops() {
    return environment -> {
      List<StopArrival> intermediateStops = getSource(environment).intermediateStops;
      if (intermediateStops == null) { return null; }
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
  public DataFetcher<Iterable<StopArrival>> intermediatePlaces() {
    return environment -> getSource(environment).intermediateStops;
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

  @Override
  public DataFetcher<String> pickupType() {
    return environment -> {
      if (getSource(environment).boardRule == null) { return "SCHEDULED"; }
      switch (getSource(environment).boardRule) {
        case "impossible": return "NONE";
        case "mustPhone": return "CALL_AGENCY";
        case "coordinateWithDriver": return "COORDINATE_WITH_DRIVER";
        default: return "SCHEDULED";
      }
    };
  }

  @Override
  public DataFetcher<String> dropoffType() {
    return environment -> {
      if (getSource(environment).alightRule == null) { return "SCHEDULED"; }
      switch (getSource(environment).alightRule) {
        case "impossible": return "NONE";
        case "mustPhone": return "CALL_AGENCY";
        case "coordinateWithDriver": return "COORDINATE_WITH_DRIVER";
        default: return "SCHEDULED";
      }
    };
  }

  @Override
  public DataFetcher<Boolean> interlineWithPreviousLeg() {
    return environment -> getSource(environment).interlineWithPreviousLeg;
  }

  @Override
  public DataFetcher<BookingInfo> dropOffBookingInfo() {
    return environment -> getSource(environment).dropOffBookingInfo;
  }

  @Override
  public DataFetcher<BookingInfo> pickupBookingInfo() {
    return environment -> getSource(environment).pickupBookingInfo;
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private Leg getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
