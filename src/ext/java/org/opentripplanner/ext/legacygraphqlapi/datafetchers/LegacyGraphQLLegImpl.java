package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.api.mapping.LocalDateMapper;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;

public class LegacyGraphQLLegImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLLeg {

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> getSource(environment).getAgency();
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
  public DataFetcher<String> dropoffType() {
    return environment -> {
      if (getSource(environment).getAlightRule() == null) {
        return PickDrop.SCHEDULED.name();
      }
      return getSource(environment).getAlightRule().name();
    };
  }

  @Override
  public DataFetcher<Double> duration() {
    return environment -> (double) getSource(environment).getDuration().toSeconds();
  }

  @Override
  public DataFetcher<Long> endTime() {
    return environment -> getSource(environment).getEndTime().toInstant().toEpochMilli();
  }

  @Override
  public DataFetcher<StopArrival> from() {
    return environment -> {
      Leg source = getSource(environment);
      return new StopArrival(
        source.getFrom(),
        source.getStartTime(),
        source.getStartTime(),
        source.getBoardStopPosInPattern(),
        source.getBoardingGtfsStopSequence()
      );
    };
  }

  @Override
  public DataFetcher<Integer> generalizedCost() {
    return environment -> getSource(environment).getGeneralizedCost();
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
  public DataFetcher<String> pickupType() {
    return environment -> {
      if (getSource(environment).getBoardRule() == null) {
        return PickDrop.SCHEDULED.name();
      }
      return getSource(environment).getBoardRule().name();
    };
  }

  @Override
  public DataFetcher<Boolean> realTime() {
    return environment -> getSource(environment).getRealTime();
  }

  // TODO
  @Override
  public DataFetcher<String> realtimeState() {
    return environment -> null;
  }

  @Override
  public DataFetcher<Boolean> rentedBike() {
    return environment -> getSource(environment).getRentedVehicle();
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
      return new StopArrival(
        source.getTo(),
        source.getEndTime(),
        source.getEndTime(),
        source.getAlightStopPosInPattern(),
        source.getAlightGtfsStopSequence()
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
}
