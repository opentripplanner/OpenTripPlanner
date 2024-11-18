package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.apis.gtfs.GraphQLUtils.stopTimeToInt;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.ZonedDateTime;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.timetable.CallTime;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class StopCallImpl implements GraphQLDataFetchers.GraphQLStopCall {

  @Override
  public DataFetcher<CallTime> arrival() {
    return environment -> {
      var tripTime = getSource(environment);
      var scheduledTime = getZonedDateTime(environment, tripTime.getScheduledArrival());
      if (scheduledTime == null) {
        return null;
      }
      return tripTime.isRealtime()
        ? CallTime.of(scheduledTime, tripTime.getArrivalDelay())
        : CallTime.ofStatic(scheduledTime);
    };
  }

  @Override
  public DataFetcher<CallTime> departure() {
    return environment -> {
      var tripTime = getSource(environment);
      var scheduledTime = getZonedDateTime(environment, tripTime.getScheduledDeparture());
      if (scheduledTime == null) {
        return null;
      }
      return tripTime.isRealtime()
        ? CallTime.of(scheduledTime, tripTime.getDepartureDelay())
        : CallTime.ofStatic(scheduledTime);
    };
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> getSource(environment).getStop();
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
