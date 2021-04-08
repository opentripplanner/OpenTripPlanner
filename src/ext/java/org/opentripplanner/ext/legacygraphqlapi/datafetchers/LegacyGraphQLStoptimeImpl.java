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
    return environment -> getRoutingService(environment).getStopForId(getSource(environment).getStopId());
  }

  @Override
  public DataFetcher<Integer> scheduledArrival() {
    return environment -> getSource(environment).getScheduledArrival();
  }

  @Override
  public DataFetcher<Integer> realtimeArrival() {
    return environment -> getSource(environment).getRealtimeArrival();
  }

  @Override
  public DataFetcher<Integer> arrivalDelay() {
    return environment -> getSource(environment).getArrivalDelay();
  }

  @Override
  public DataFetcher<Integer> scheduledDeparture() {
    return environment -> getSource(environment).getScheduledDeparture();
  }

  @Override
  public DataFetcher<Integer> realtimeDeparture() {
    return environment -> getSource(environment).getRealtimeDeparture();
  }

  @Override
  public DataFetcher<Integer> departureDelay() {
    return environment -> getSource(environment).getDepartureDelay();
  }

  @Override
  public DataFetcher<Boolean> timepoint() {
    return environment -> getSource(environment).isTimepoint();
  }

  @Override
  public DataFetcher<Boolean> realtime() {
    return environment -> getSource(environment).isRealtime();
  }

  @Override
  public DataFetcher<String> realtimeState() {
    return environment -> getSource(environment).getRealtimeState().name();
  }

  @Override
  public DataFetcher<String> pickupType() {
    return environment -> {
      switch (getSource(environment).getPickupType()) {
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
      switch (getSource(environment).getDropoffType()) {
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
    return environment -> getSource(environment).getServiceDay();
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getSource(environment).getTrip();
  }

  @Override
  public DataFetcher<String> headsign() {
    return environment -> getSource(environment).getHeadsign();
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private TripTimeShort getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
