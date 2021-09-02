package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.Fare;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LegacyGraphQLItineraryImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLItinerary {

  @Override
  public DataFetcher<Long> startTime() {
    return environment -> getSource(environment).startTime().getTime().getTime();
  }

  @Override
  public DataFetcher<Long> endTime() {
    return environment -> getSource(environment).endTime().getTime().getTime();
  }

  @Override
  public DataFetcher<Long> duration() {
    return environment -> (long) getSource(environment).durationSeconds;
  }

  @Override
  public DataFetcher<Integer> generalizedCost() {
    return environment -> getSource(environment).generalizedCost;
  }

  @Override
  public DataFetcher<Long> waitingTime() {
    return environment -> (long) getSource(environment).waitingTimeSeconds;
  }

  @Override
  public DataFetcher<Long> walkTime() {
    return environment -> (long) getSource(environment).nonTransitTimeSeconds;
  }

  @Override
  public DataFetcher<Double> walkDistance() {
    return environment -> getSource(environment).nonTransitDistanceMeters;
  }

  @Override
  public DataFetcher<Iterable<Leg>> legs() {
    return environment -> getSource(environment).legs;
  }

  @Override
  public DataFetcher<Iterable<Map<String, Object>>> fares() {
    return environment -> {
      Fare fare = getSource(environment).fare;
      if (fare == null) {
        return null;
      }
      List<Map<String, Object>> results = fare.fare.keySet().stream().map(fareKey -> {
        Map<String, Object> result = new HashMap<>();
        result.put("name", fareKey.name());
        result.put("fare", fare.getFare(fareKey));
        result.put("details", fare.getDetails(fareKey));
        return result;
      }).collect(Collectors.toList());
      return results;
    };
  }

  @Override
  public DataFetcher<Double> elevationGained() {
    return environment -> getSource(environment).elevationGained;
  }

  @Override
  public DataFetcher<Double> elevationLost() {
    return environment -> getSource(environment).elevationLost;
  }

  @Override
  public DataFetcher<Boolean> arrivedAtDestinationWithRentedBicycle() {
    return environment -> getSource(environment).arrivedAtDestinationWithRentedBicycle;
  }

  @Override
  public DataFetcher<Iterable<SystemNotice>> systemNotices() {
    return environment -> getSource(environment).systemNotices;
  }

  private Itinerary getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
