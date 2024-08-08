package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.apis.gtfs.GraphQLUtils.stopTimeToInt;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;

public class StoptimeImpl implements GraphQLDataFetchers.GraphQLStoptime {

  @Override
  public DataFetcher<Integer> arrivalDelay() {
    return environment -> stopTimeToInt(getSource(environment).getArrivalDelay());
  }

  @Override
  public DataFetcher<Integer> departureDelay() {
    return environment -> getSource(environment).getDepartureDelay();
  }

  @Override
  public DataFetcher<String> dropoffType() {
    return environment ->
      switch (getSource(environment).getDropoffType()) {
        case SCHEDULED -> "SCHEDULED";
        case NONE -> "NONE";
        case CALL_AGENCY -> "CALL_AGENCY";
        case COORDINATE_WITH_DRIVER -> "COORDINATE_WITH_DRIVER";
        case CANCELLED -> null;
      };
  }

  @Override
  public DataFetcher<String> headsign() {
    return environment ->
      GraphQLUtils.getTranslation(getSource(environment).getHeadsign(), environment);
  }

  @Override
  public DataFetcher<String> pickupType() {
    return environment ->
      switch (getSource(environment).getPickupType()) {
        case SCHEDULED -> "SCHEDULED";
        case NONE -> "NONE";
        case CALL_AGENCY -> "CALL_AGENCY";
        case COORDINATE_WITH_DRIVER -> "COORDINATE_WITH_DRIVER";
        case CANCELLED -> null;
      };
  }

  @Override
  public DataFetcher<Boolean> realtime() {
    return environment -> getSource(environment).isRealtime();
  }

  @Override
  public DataFetcher<Integer> realtimeArrival() {
    return environment -> stopTimeToInt(getSource(environment).getRealtimeArrival());
  }

  @Override
  public DataFetcher<Integer> realtimeDeparture() {
    return environment -> stopTimeToInt(getSource(environment).getRealtimeDeparture());
  }

  @Override
  public DataFetcher<String> realtimeState() {
    return environment ->
      getSource(environment).isCanceledEffectively()
        ? RealTimeState.CANCELED.name()
        : getSource(environment).getRealTimeState().name();
  }

  @Override
  public DataFetcher<Integer> scheduledArrival() {
    return environment -> stopTimeToInt(getSource(environment).getScheduledArrival());
  }

  @Override
  public DataFetcher<Integer> scheduledDeparture() {
    return environment -> stopTimeToInt(getSource(environment).getScheduledDeparture());
  }

  @Override
  public DataFetcher<Integer> stopPosition() {
    return environment -> getSource(environment).getGtfsSequence();
  }

  @Override
  public DataFetcher<Long> serviceDay() {
    return environment -> getSource(environment).getServiceDayMidnight();
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).getStop();
  }

  @Override
  public DataFetcher<Boolean> timepoint() {
    return environment -> getSource(environment).isTimepoint();
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment -> getSource(environment).getTrip();
  }

  private TripTimeOnDate getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
