package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;

public class StoptimeImpl implements GraphQLDataFetchers.GraphQLStoptime {

  @Override
  public DataFetcher<Integer> arrivalDelay() {
    return environment -> missingValueToNull(getSource(environment).getArrivalDelay());
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
    return environment -> missingValueToNull(getSource(environment).getRealtimeArrival());
  }

  @Override
  public DataFetcher<Integer> realtimeDeparture() {
    return environment -> missingValueToNull(getSource(environment).getRealtimeDeparture());
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
    return environment -> missingValueToNull(getSource(environment).getScheduledArrival());
  }

  @Override
  public DataFetcher<Integer> scheduledDeparture() {
    return environment -> missingValueToNull(getSource(environment).getScheduledDeparture());
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

  /**
   * Generally the missing values are removed during the graph build. However, for flex trips they
   * are not and have to be converted to null here.
   */
  private Integer missingValueToNull(int value) {
    if (value == StopTime.MISSING_VALUE) {
      return null;
    } else {
      return value;
    }
  }
}
