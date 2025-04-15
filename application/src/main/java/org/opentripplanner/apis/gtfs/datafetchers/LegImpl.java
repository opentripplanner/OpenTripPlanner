package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.LocalDateMapper;
import org.opentripplanner.apis.gtfs.mapping.NumberMapper;
import org.opentripplanner.apis.gtfs.mapping.PickDropMapper;
import org.opentripplanner.apis.gtfs.mapping.RealtimeStateMapper;
import org.opentripplanner.ext.ridehailing.model.RideEstimate;
import org.opentripplanner.ext.ridehailing.model.RideHailingLeg;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.LegCallTime;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.model.plan.legreference.LegReferenceSerializer;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.alternativelegs.AlternativeLegs;
import org.opentripplanner.routing.alternativelegs.AlternativeLegsFilter;
import org.opentripplanner.routing.alternativelegs.NavigationDirection;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

public class LegImpl implements GraphQLDataFetchers.GraphQLLeg {

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> getSource(environment).getAgency();
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment -> getSource(environment).getTransitAlerts();
  }

  @Override
  public DataFetcher<Integer> arrivalDelay() {
    return environment -> getSource(environment).getArrivalDelay();
  }

  @Override
  public DataFetcher<Integer> departureDelay() {
    return environment -> getSource(environment).getDepartureDelay();
  }

  @Override
  public DataFetcher<Double> distance() {
    return environment -> getSource(environment).getDistanceMeters();
  }

  @Override
  public DataFetcher<BookingInfo> dropOffBookingInfo() {
    return environment -> getSource(environment).getDropOffBookingInfo();
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLPickupDropoffType> dropoffType() {
    return environment -> {
      if (getSource(environment).getAlightRule() == null) {
        return GraphQLTypes.GraphQLPickupDropoffType.SCHEDULED;
      }
      return PickDropMapper.map(getSource(environment).getAlightRule());
    };
  }

  @Override
  public DataFetcher<Double> duration() {
    return environment -> (double) getSource(environment).getDuration().toSeconds();
  }

  @Override
  public DataFetcher<LegCallTime> end() {
    return environment -> getSource(environment).end();
  }

  @Override
  @Deprecated
  public DataFetcher<Long> endTime() {
    return environment -> getSource(environment).getEndTime().toInstant().toEpochMilli();
  }

  @Override
  public DataFetcher<Iterable<FareProductUse>> fareProducts() {
    return environment -> getSource(environment).fareProducts();
  }

  @Override
  public DataFetcher<StopArrival> from() {
    return environment -> {
      Leg source = getSource(environment);
      var boardRule = source.getBoardRule();
      return new StopArrival(
        source.getFrom(),
        source.start(),
        source.start(),
        source.getBoardStopPosInPattern(),
        source.getBoardingGtfsStopSequence(),
        boardRule != null && boardRule.isNotRoutable()
      );
    };
  }

  @Override
  public DataFetcher<Integer> generalizedCost() {
    return environment -> getSource(environment).getGeneralizedCost();
  }

  @Override
  public DataFetcher<String> headsign() {
    return environment ->
      GraphQLUtils.getTranslation(getSource(environment).getHeadsign(), environment);
  }

  @Override
  public DataFetcher<Boolean> interlineWithPreviousLeg() {
    return environment -> getSource(environment).isInterlinedWithPreviousLeg();
  }

  // TODO
  @Override
  public DataFetcher<Boolean> intermediatePlace() {
    return environment -> false;
  }

  @Override
  public DataFetcher<Iterable<StopArrival>> intermediatePlaces() {
    return environment -> getSource(environment).getIntermediateStops();
  }

  @Override
  public DataFetcher<Iterable<Object>> intermediateStops() {
    return environment -> {
      List<StopArrival> intermediateStops = getSource(environment).getIntermediateStops();
      if (intermediateStops == null) {
        return null;
      }
      return intermediateStops
        .stream()
        .map(intermediateStop -> intermediateStop.place.stop)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    };
  }

  @Override
  public DataFetcher<Geometry> legGeometry() {
    return environment -> getSource(environment).getLegGeometry();
  }

  @Override
  public DataFetcher<String> mode() {
    return environment -> {
      Leg leg = getSource(environment);
      if (leg instanceof StreetLeg s) {
        return s.getMode().name();
      }
      if (leg instanceof TransitLeg s) {
        return s.getMode().name();
      }
      throw new IllegalStateException("Unhandled leg type: " + leg);
    };
  }

  @Override
  public DataFetcher<BookingInfo> pickupBookingInfo() {
    return environment -> getSource(environment).getPickupBookingInfo();
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLPickupDropoffType> pickupType() {
    return environment -> {
      if (getSource(environment).getBoardRule() == null) {
        return GraphQLTypes.GraphQLPickupDropoffType.SCHEDULED;
      }
      return PickDropMapper.map(getSource(environment).getBoardRule());
    };
  }

  @Override
  public DataFetcher<Boolean> realTime() {
    return environment -> getSource(environment).isRealTimeUpdated();
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLRealtimeState> realtimeState() {
    return environment -> {
      var state = getSource(environment).getRealTimeState();
      return RealtimeStateMapper.map(state);
    };
  }

  @Override
  public DataFetcher<Boolean> rentedBike() {
    return environment -> getSource(environment).getRentedVehicle();
  }

  @Override
  public DataFetcher<RideEstimate> rideHailingEstimate() {
    return environment -> {
      Leg leg = getSource(environment);
      if (leg instanceof RideHailingLeg rhl) {
        return rhl.rideEstimate();
      } else {
        return null;
      }
    };
  }

  @Override
  public DataFetcher<Route> route() {
    return environment -> getSource(environment).getRoute();
  }

  @Override
  public DataFetcher<String> serviceDate() {
    return environment -> LocalDateMapper.mapToApi(getSource(environment).getServiceDate());
  }

  @Override
  public DataFetcher<LegCallTime> start() {
    return environment -> getSource(environment).start();
  }

  @Override
  @Deprecated
  public DataFetcher<Long> startTime() {
    return environment -> getSource(environment).getStartTime().toInstant().toEpochMilli();
  }

  @Override
  public DataFetcher<Iterable<WalkStep>> steps() {
    return environment -> getSource(environment).getWalkSteps();
  }

  @Override
  public DataFetcher<StopArrival> to() {
    return environment -> {
      Leg source = getSource(environment);
      var alightRule = source.getAlightRule();
      return new StopArrival(
        source.getTo(),
        source.end(),
        source.end(),
        source.getAlightStopPosInPattern(),
        source.getAlightGtfsStopSequence(),
        alightRule != null && alightRule.isNotRoutable()
      );
    };
  }

  @Override
  public DataFetcher<Boolean> transitLeg() {
    return environment -> getSource(environment).isTransitLeg();
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getSource(environment).getTrip();
  }

  @Override
  public DataFetcher<Boolean> walkingBike() {
    return environment -> getSource(environment).getWalkingBike();
  }

  private Leg getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }

  @Override
  public DataFetcher<Iterable<Leg>> previousLegs() {
    return alternativeLegs(NavigationDirection.PREVIOUS);
  }

  @Override
  public DataFetcher<Iterable<Leg>> nextLegs() {
    return alternativeLegs(NavigationDirection.NEXT);
  }

  private DataFetcher<Iterable<Leg>> alternativeLegs(NavigationDirection direction) {
    return environment -> {
      if (environment.getSource() instanceof ScheduledTransitLeg originalLeg) {
        var args = new GraphQLTypes.GraphQLLegNextLegsArgs(environment.getArguments());

        int numberOfLegs = args.getGraphQLNumberOfLegs();

        var originModesWithParentStation = args.getGraphQLOriginModesWithParentStation();
        var destinationModesWithParentStation = args.getGraphQLDestinationModesWithParentStation();

        boolean limitToExactOriginStop =
          originModesWithParentStation == null ||
          !(originModesWithParentStation
              .stream()
              .map(GraphQLTypes.GraphQLTransitMode::toString)
              .toList()
              .contains(originalLeg.getMode().name()));

        boolean limitToExactDestinationStop =
          destinationModesWithParentStation == null ||
          !(destinationModesWithParentStation
              .stream()
              .map(GraphQLTypes.GraphQLTransitMode::toString)
              .toList()
              .contains(originalLeg.getMode().name()));

        var res = AlternativeLegs.getAlternativeLegs(
          environment.getSource(),
          numberOfLegs,
          environment.<GraphQLRequestContext>getContext().transitService(),
          direction,
          AlternativeLegsFilter.NO_FILTER,
          limitToExactOriginStop,
          limitToExactDestinationStop
        )
          .stream()
          .map(Leg.class::cast)
          .toList();
        return res;
      } else return null;
    };
  }

  @Override
  public DataFetcher<Double> accessibilityScore() {
    return environment -> NumberMapper.toDouble(getSource(environment).accessibilityScore());
  }

  @Override
  public DataFetcher<String> id() {
    return environment -> {
      var ref = getSource(environment).getLegReference();
      if (ref == null) {
        return null;
      }
      return LegReferenceSerializer.encode(ref);
    };
  }
}
