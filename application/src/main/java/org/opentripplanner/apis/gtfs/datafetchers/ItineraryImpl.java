package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.mapping.NumberMapper;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public class ItineraryImpl implements GraphQLDataFetchers.GraphQLItinerary {

  /// The key used to store the itinerary in the GraphQL context for looking it up during
  /// the fare resolution in [LegImpl#fareProducts].
  static final String ITINERARY_CONTEXT_KEY = "itinerary";

  @Override
  public DataFetcher<Boolean> arrivedAtDestinationWithRentedBicycle() {
    return environment -> getSource(environment).isArrivedAtDestinationWithRentedVehicle();
  }

  @Override
  public DataFetcher<Long> duration() {
    return environment -> (long) getSource(environment).totalDuration().toSeconds();
  }

  @Override
  public DataFetcher<Double> elevationGained() {
    return environment -> getSource(environment).totalElevationGained();
  }

  @Override
  public DataFetcher<Double> elevationLost() {
    return environment -> getSource(environment).totalElevationLost();
  }

  @Override
  public DataFetcher<OffsetDateTime> end() {
    return environment -> getSource(environment).endTime().toOffsetDateTime();
  }

  @Deprecated
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
    return environment -> getSource(environment).generalizedCost();
  }

  @Override
  @SuppressWarnings("unchecked")
  public DataFetcher<Iterable<Leg>> legs() {
    // Returns a DataFetcherResult so child resolvers (e.g. fareProducts) can look up the parent
    // itinerary via localContext. The existing locale map from the parent query is preserved so
    // that translation-aware resolvers downstream continue to work. The cast is to the raw
    // fetcher type to avoid a JVM checkcast to Iterable — graphql-java handles DataFetcherResult
    // at runtime without requiring the type to match.
    DataFetcher<?> fetcher = environment -> {
      var itinerary = getSource(environment);
      var ctx = new HashMap<String, Object>();
      Map<String, ?> parentCtx = environment.getLocalContext();
      if (parentCtx != null) {
        ctx.putAll(parentCtx);
      }
      ctx.put(ITINERARY_CONTEXT_KEY, itinerary);
      return DataFetcherResult.<Iterable<Leg>>newResult()
        .data(itinerary.legs())
        .localContext(ctx)
        .build();
    };
    return (DataFetcher<Iterable<Leg>>) fetcher;
  }

  @Override
  public DataFetcher<Integer> numberOfTransfers() {
    return environment -> getSource(environment).numberOfTransfers();
  }

  @Override
  public DataFetcher<OffsetDateTime> start() {
    return environment -> getSource(environment).startTime().toOffsetDateTime();
  }

  @Deprecated
  @Override
  public DataFetcher<Long> startTime() {
    return environment -> getSource(environment).startTime().toInstant().toEpochMilli();
  }

  @Override
  public DataFetcher<Iterable<SystemNotice>> systemNotices() {
    return environment -> getSource(environment).systemNotices();
  }

  @Override
  public DataFetcher<Long> waitingTime() {
    return environment -> (long) getSource(environment).totalWaitingDuration().toSeconds();
  }

  @Override
  public DataFetcher<Double> walkDistance() {
    return environment -> getSource(environment).totalWalkDistanceMeters();
  }

  @Override
  public DataFetcher<Long> walkTime() {
    return environment -> (long) getSource(environment).totalWalkDuration().toSeconds();
  }

  @Override
  public DataFetcher<Double> accessibilityScore() {
    return environment -> NumberMapper.toDouble(getSource(environment).accessibilityScore());
  }

  @Override
  public DataFetcher<Emission> emissionsPerPerson() {
    return environment -> getSource(environment).emissionPerPerson();
  }

  private Itinerary getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
