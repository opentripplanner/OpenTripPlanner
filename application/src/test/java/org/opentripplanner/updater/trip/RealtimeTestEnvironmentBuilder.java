package org.opentripplanner.updater.trip;

import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.TransitModel;

public class RealtimeTestEnvironmentBuilder implements RealtimeTestConstants {

  private RealtimeTestEnvironment.SourceType sourceType;
  private final TransitModel transitModel = new TransitModel(STOP_MODEL, new Deduplicator());

  RealtimeTestEnvironmentBuilder withSourceType(RealtimeTestEnvironment.SourceType sourceType) {
    this.sourceType = sourceType;
    return this;
  }

  public RealtimeTestEnvironmentBuilder addTrip(TripInput trip) {
    createTrip(trip);
    transitModel.index();
    return this;
  }

  public RealtimeTestEnvironment build() {
    Objects.requireNonNull(sourceType, "sourceType cannot be null");
    transitModel.initTimeZone(TIME_ZONE);
    transitModel.addAgency(TransitModelForTest.AGENCY);

    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(
      SERVICE_ID,
      List.of(SERVICE_DATE.minusDays(1), SERVICE_DATE, SERVICE_DATE.plusDays(1))
    );
    transitModel.getServiceCodes().put(SERVICE_ID, 0);
    transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);

    return new RealtimeTestEnvironment(sourceType, transitModel);
  }

  private Trip createTrip(TripInput tripInput) {
    var trip = Trip
      .of(id(tripInput.id()))
      .withRoute(tripInput.route())
      .withHeadsign(I18NString.of("Headsign of %s".formatted(tripInput.id())))
      .withServiceId(SERVICE_ID)
      .build();

    var tripOnServiceDate = TripOnServiceDate
      .of(trip.getId())
      .withTrip(trip)
      .withServiceDate(SERVICE_DATE)
      .build();

    transitModel.addTripOnServiceDate(tripOnServiceDate);

    if (tripInput.route().getOperator() != null) {
      transitModel.addOperators(List.of(tripInput.route().getOperator()));
    }

    var stopTimes = IntStream
      .range(0, tripInput.stops().size())
      .mapToObj(i -> {
        var stop = tripInput.stops().get(i);
        return createStopTime(trip, i, stop.stop(), stop.arrivalTime(), stop.departureTime());
      })
      .toList();

    TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, null);

    final TripPattern pattern = TransitModelForTest
      .tripPattern(tripInput.id() + "Pattern", tripInput.route())
      .withStopPattern(
        TransitModelForTest.stopPattern(
          tripInput.stops().stream().map(TripInput.StopCall::stop).toList()
        )
      )
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();

    transitModel.addTripPattern(pattern.getId(), pattern);

    return trip;
  }

  private static StopTime createStopTime(
    Trip trip,
    int stopSequence,
    StopLocation stop,
    int arrivalTime,
    int departureTime
  ) {
    var st = new StopTime();
    st.setTrip(trip);
    st.setStopSequence(stopSequence);
    st.setStop(stop);
    st.setArrivalTime(arrivalTime);
    st.setDepartureTime(departureTime);
    return st;
  }
}
