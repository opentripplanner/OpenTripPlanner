package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.apis.gtfs.GraphQLUtils.stopTimeToInt;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.ZonedDateTime;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.apis.gtfs.GtfsGraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.CallRealTime;
import org.opentripplanner.apis.gtfs.model.CallSchedule;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.ArrivalDepartureTime;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.TimeWindow;
import org.opentripplanner.apis.gtfs.model.DestinationDisplay;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.timetable.EstimatedTime;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class StopCallImpl implements GraphQLDataFetchers.GraphQLStopCall {

  @Override
  public DataFetcher<DestinationDisplay> destinationDisplay() {
    return environment -> {
      var headsign = getSource(environment).getHeadsign();
      return headsign == null ? null : new DestinationDisplay(headsign);
    };
  }

  @Override
  public DataFetcher<Geometry> geometryToNextCall() {
    return environment -> {
      var tripTime = getSource(environment);
      var pattern = tripTime.pattern();
      var stopPosition = tripTime.getStopPosition();
      if (stopPosition >= pattern.numberOfStops() - 1) {
        return null;
      }
      // The path of a flexible trip is not known
      var flexIndex = getTransitService(environment).getFlexIndex();
      if (flexIndex != null && flexIndex.getTripById(tripTime.getTrip().getId()) != null) {
        return null;
      }
      return pattern.getHopGeometry(stopPosition);
    };
  }

  @Override
  public DataFetcher<CallRealTime> realTime() {
    return environment -> {
      var tripTime = getSource(environment);
      var canceled = tripTime.isCancelledStop();
      // A canceled call is reported even if the trip times are not marked as updated
      if (!tripTime.isRealtime() && !canceled) {
        return null;
      }
      var scheduledArrival = getZonedDateTime(environment, tripTime.getScheduledArrival());
      var estimatedArrival =
        scheduledArrival == null
          ? null
          : EstimatedTime.of(scheduledArrival, tripTime.getArrivalDelay());
      var scheduledDeparture = getZonedDateTime(environment, tripTime.getScheduledDeparture());
      var estimatedDeparture =
        scheduledDeparture == null
          ? null
          : EstimatedTime.of(scheduledDeparture, tripTime.getDepartureDelay());
      return new CallRealTime(estimatedArrival, estimatedDeparture, canceled);
    };
  }

  @Override
  public DataFetcher<CallSchedule> schedule() {
    return environment -> {
      var tripTime = getSource(environment);

      if (tripTime.hasScheduledTimes()) {
        var scheduledArrival = getZonedDateTime(environment, tripTime.getScheduledArrival());
        var scheduledDeparture = getZonedDateTime(environment, tripTime.getScheduledDeparture());
        // This is quite awkward: the call only knows its (possibly real-time) pattern, so the
        // scheduled pattern has to be looked up separately and the stop position translated to it
        // to get the scheduled pickup/drop off type. This should be refactored when the transit
        // model is refactored.
        var scheduledPattern = getTransitService(environment).findPattern(tripTime.getTrip());
        return new CallSchedule(
          new ArrivalDepartureTime(scheduledArrival, scheduledDeparture, tripTime.isTimepoint()),
          tripTime.getScheduledPickupType(scheduledPattern),
          tripTime.getScheduledDropoffType(scheduledPattern)
        );
      } else {
        var transitService = getTransitService(environment);
        var flexTrip = transitService.getFlexIndex().getTripById(tripTime.getTrip().getId());
        if (flexTrip == null) {
          return null;
        }
        var stopPosition = tripTime.getStopPosition();
        var startSecs = flexTrip.earliestDepartureTime(stopPosition);
        var endSecs = flexTrip.latestArrivalTime(stopPosition);

        var serviceDay = tripTime.getServiceDay();
        var timeZone = transitService.getTimeZone();
        var start = ServiceDateUtils.toOffsetDateTime(serviceDay, timeZone, startSecs);
        var end = ServiceDateUtils.toOffsetDateTime(serviceDay, timeZone, endSecs);

        return new CallSchedule(
          new TimeWindow(start, end),
          flexTrip.getBoardRule(stopPosition),
          flexTrip.getAlightRule(stopPosition)
        );
      }
    };
  }

  @Override
  public DataFetcher<Iterable<Notice>> notices() {
    return env -> {
      var call = getSource(env);
      var transitService = getTransitService(env);
      return transitService.findNotices(call.getStopTimeKey());
    };
  }

  @Override
  public DataFetcher<Object> stopLocation() {
    return environment -> getSource(environment).getStop();
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GtfsGraphQLRequestContext>getContext().transitService();
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
