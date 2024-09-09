package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.apis.gtfs.GraphQLUtils.stopTimeToInt;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.ZonedDateTime;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.PickDropMapper;
import org.opentripplanner.apis.gtfs.model.ArrivalDepartureTime;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.service.TransitService;

public class ExactDatedStopTimeImpl implements GraphQLDataFetchers.GraphQLExactDatedStopTime {

  @Override
  public DataFetcher<ArrivalDepartureTime> arrival() {
    return environment -> {
      var tripTime = getSource(environment);
      var scheduledTime = getZonedDateTime(environment, tripTime.getScheduledArrival());
      if (scheduledTime == null) {
        return null;
      }
      return tripTime.isRealtime()
        ? ArrivalDepartureTime.of(scheduledTime, tripTime.getArrivalDelay())
        : ArrivalDepartureTime.ofStatic(scheduledTime);
    };
  }

  @Override
  public DataFetcher<ArrivalDepartureTime> departure() {
    return environment -> {
      var tripTime = getSource(environment);
      var scheduledTime = getZonedDateTime(environment, tripTime.getScheduledDeparture());
      if (scheduledTime == null) {
        return null;
      }
      return tripTime.isRealtime()
        ? ArrivalDepartureTime.of(scheduledTime, tripTime.getDepartureDelay())
        : ArrivalDepartureTime.ofStatic(scheduledTime);
    };
  }

  @Override
  public DataFetcher<String> dropoffType() {
    return environment -> PickDropMapper.map(getSource(environment).getDropoffType());
  }

  @Override
  public DataFetcher<String> headsign() {
    return environment ->
      GraphQLUtils.getTranslation(getSource(environment).getHeadsign(), environment);
  }

  @Override
  public DataFetcher<String> pickupType() {
    return environment -> PickDropMapper.map(getSource(environment).getPickupType());
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLStopRealTimeState> realtimeState() {
    return environment -> {
      var tripTime = getSource(environment);
      // TODO support ADDED state
      if (tripTime.isCanceledEffectively()) {
        return GraphQLTypes.GraphQLStopRealTimeState.CANCELED;
      }
      if (tripTime.isNoDataStop()) {
        return GraphQLTypes.GraphQLStopRealTimeState.NO_DATA;
      }
      if (tripTime.isRecordedStop()) {
        return GraphQLTypes.GraphQLStopRealTimeState.RECORDED;
      }
      if (tripTime.isRealtime()) {
        return GraphQLTypes.GraphQLStopRealTimeState.UPDATED;
      }
      return GraphQLTypes.GraphQLStopRealTimeState.UNUPDATED;
    };
  }

  @Override
  public DataFetcher<Integer> stopPosition() {
    return environment -> getSource(environment).getGtfsSequence();
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).getStop();
  }

  @Override
  public DataFetcher<Boolean> timepoint() {
    return environment -> getSource(environment).isTimepoint();
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private ZonedDateTime getZonedDateTime(DataFetchingEnvironment environment, int time) {
    var fixedTime = stopTimeToInt(time);
    if (fixedTime == null) {
      return null;
    }
    var serviceDate = getSource(environment).getServiceDay();
    TransitService transitService = getTransitService(environment);
    return ServiceDateUtils.toZonedDateTime(serviceDate, transitService.getTimeZone(), fixedTime);
  }

  private TripTimeOnDate getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
