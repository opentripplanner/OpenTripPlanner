package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.apis.gtfs.GraphQLUtils.stopTimeToInt;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.ZonedDateTime;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.plan.FixedArrivalDepartureTime;
import org.opentripplanner.transit.service.TransitService;

public class FixedStopTimeOnServiceDateImpl
  implements GraphQLDataFetchers.GraphQLFixedStopTimeOnServiceDate {

  @Override
  public DataFetcher<FixedArrivalDepartureTime> arrival() {
    return environment -> {
      var tripTime = getSource(environment);
      var scheduledTime = getZonedDateTime(environment, tripTime.getScheduledArrival());
      if (scheduledTime == null) {
        return null;
      }
      return tripTime.isRealtime()
        ? FixedArrivalDepartureTime.of(scheduledTime, tripTime.getArrivalDelay())
        : FixedArrivalDepartureTime.ofStatic(scheduledTime);
    };
  }

  @Override
  public DataFetcher<FixedArrivalDepartureTime> departure() {
    return environment -> {
      var tripTime = getSource(environment);
      var scheduledTime = getZonedDateTime(environment, tripTime.getScheduledDeparture());
      if (scheduledTime == null) {
        return null;
      }
      return tripTime.isRealtime()
        ? FixedArrivalDepartureTime.of(scheduledTime, tripTime.getDepartureDelay())
        : FixedArrivalDepartureTime.ofStatic(scheduledTime);
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
