package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.mapping.NumberMapper;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Emissions;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class ItineraryImpl implements GraphQLDataFetchers.GraphQLItinerary {

  @Override
  public DataFetcher<Boolean> arrivedAtDestinationWithRentedBicycle() {
    return environment -> getSource(environment).isArrivedAtDestinationWithRentedVehicle();
  }

  @Override
  public DataFetcher<Long> duration() {
    return environment -> (long) getSource(environment).getDuration().toSeconds();
  }

  @Override
  public DataFetcher<Double> elevationGained() {
    return environment -> getSource(environment).getElevationGained();
  }

  @Override
  public DataFetcher<Double> elevationLost() {
    return environment -> getSource(environment).getElevationLost();
  }

  @Override
  public DataFetcher<Long> endTime() {
    return environment -> getSource(environment).endTime().toInstant().toEpochMilli();
  }

  @Override
  public DataFetcher<Iterable<Object>> fares() {
    return environment -> List.of();
  }

  @Override
  public DataFetcher<Integer> generalizedCost() {
    return environment -> getSource(environment).getGeneralizedCost();
  }

  @Override
  public DataFetcher<Iterable<Leg>> legs() {
    return environment -> getSource(environment).getLegs();
  }

  @Override
  public DataFetcher<Integer> numberOfTransfers() {
    return environment -> getSource(environment).getNumberOfTransfers();
  }

  @Override
  public DataFetcher<Long> startTime() {
    return environment -> getSource(environment).startTime().toInstant().toEpochMilli();
  }

  @Override
  public DataFetcher<Iterable<SystemNotice>> systemNotices() {
    return environment -> getSource(environment).getSystemNotices();
  }

  @Override
  public DataFetcher<Long> waitingTime() {
    return environment -> (long) getSource(environment).getWaitingDuration().toSeconds();
  }

  @Override
  public DataFetcher<Double> walkDistance() {
    return environment -> getSource(environment).walkDistanceMeters();
  }

  @Override
  public DataFetcher<Long> walkTime() {
    return environment -> (long) getSource(environment).walkDuration().toSeconds();
  }

  @Override
  public DataFetcher<Double> accessibilityScore() {
    return environment -> NumberMapper.toDouble(getSource(environment).getAccessibilityScore());
  }

  @Override
  public DataFetcher<Emissions> emissionsPerPerson() {
    return environment -> getSource(environment).getEmissionsPerPerson();
  }

  private Itinerary getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
