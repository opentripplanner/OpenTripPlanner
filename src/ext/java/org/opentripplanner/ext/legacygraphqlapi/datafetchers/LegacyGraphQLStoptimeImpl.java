package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.routing.RoutingService;

public class LegacyGraphQLStoptimeImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLStoptime {

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getRoutingService(environment).getStopForId(getSource(environment).stopId);
  }

  @Override
  public DataFetcher<Integer> scheduledArrival() {
    return environment -> getSource(environment).scheduledArrival;
  }

  @Override
  public DataFetcher<Integer> realtimeArrival() {
    return environment -> getSource(environment).realtimeArrival;
  }

  @Override
  public DataFetcher<Integer> arrivalDelay() {
    return environment -> getSource(environment).arrivalDelay;
  }

  @Override
  public DataFetcher<Integer> scheduledDeparture() {
    return environment -> getSource(environment).scheduledDeparture;
  }

  @Override
  public DataFetcher<Integer> realtimeDeparture() {
    return environment -> getSource(environment).realtimeDeparture;
  }

  @Override
  public DataFetcher<Integer> departureDelay() {
    return environment -> getSource(environment).departureDelay;
  }

  @Override
  public DataFetcher<Boolean> timepoint() {
    return environment -> getSource(environment).timepoint;
  }

  @Override
  public DataFetcher<Boolean> realtime() {
    return environment -> getSource(environment).realtime;
  }

  @Override
  public DataFetcher<String> realtimeState() {
    return environment -> getSource(environment).realtimeState.name();
  }

  @Override
  public DataFetcher<String> pickupType() {
    return environment -> {
      switch (getSource(environment).pickupType) {
        case 0: return "SCHEDULED";
        case 1: return "NONE";
        case 2: return "CALL_AGENCY";
        case 3: return "COORDINATE_WITH_DRIVER";
        default: return null;
      }
    };
  }

  @Override
  public DataFetcher<String> dropoffType() {
    return environment -> {
      switch (getSource(environment).dropoffType) {
        case 0: return "SCHEDULED";
        case 1: return "NONE";
        case 2: return "CALL_AGENCY";
        case 3: return "COORDINATE_WITH_DRIVER";
        default: return null;
      }
    };
  }

  @Override
  public DataFetcher<Long> serviceDay() {
    return environment -> getSource(environment).serviceDay;
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getRoutingService(environment).getTripForId().get(getSource(environment).tripId);
  }

  @Override
  public DataFetcher<String> headsign() {
    return environment -> getSource(environment).headsign;
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private TripTimeShort getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
