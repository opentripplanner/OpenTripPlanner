package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.apis.gtfs.GraphQLUtils.stopTimeToInt;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.ZonedDateTime;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.CallRealTime;
import org.opentripplanner.apis.gtfs.model.CallSchedule;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.ArrivalDepartureTime;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.TimeWindow;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.timetable.EstimatedTime;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class StopCallImpl implements GraphQLDataFetchers.GraphQLStopCall {

  @Override
  public DataFetcher<CallRealTime> realTime() {
    return environment -> {
      var tripTime = getSource(environment);
      if (!tripTime.isRealtime()) {
        return null;
      }
      var scheduledArrival = getZonedDateTime(environment, tripTime.getScheduledArrival());
      var estimatedArrival = scheduledArrival == null
        ? null
        : EstimatedTime.of(scheduledArrival, tripTime.getArrivalDelay());
      var scheduledDeparture = getZonedDateTime(environment, tripTime.getScheduledDeparture());
      var estimatedDeparture = scheduledDeparture == null
        ? null
        : EstimatedTime.of(scheduledDeparture, tripTime.getDepartureDelay());
      return new CallRealTime(estimatedArrival, estimatedDeparture);
    };
  }

  @Override
  public DataFetcher<CallSchedule> schedule() {
    return environment -> {
      var tripTime = getSource(environment);

      if (tripTime.hasScheduledTimes()) {
        var scheduledArrival = getZonedDateTime(environment, tripTime.getScheduledArrival());
        var scheduledDeparture = getZonedDateTime(environment, tripTime.getScheduledDeparture());
        return new CallSchedule(new ArrivalDepartureTime(scheduledArrival, scheduledDeparture));
      } else {
        var transitService = getTransitService(environment);
        var flexTrip = transitService.getFlexIndex().getTripById(tripTime.getTrip().getId());
        if (flexTrip == null) {
          return null;
        }
        var startSecs = flexTrip.earliestDepartureTime(tripTime.getStopIndex());
        var endSecs = flexTrip.latestArrivalTime(tripTime.getStopIndex());

        var serviceDay = tripTime.getServiceDay();
        var timeZone = transitService.getTimeZone();
        var start = ServiceDateUtils.toOffsetDateTime(serviceDay, timeZone, startSecs);
        var end = ServiceDateUtils.toOffsetDateTime(serviceDay, timeZone, endSecs);

        return new CallSchedule(new TimeWindow(start, end));
      }
    };
  }

  @Override
  public DataFetcher<Object> stopLocation() {
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
