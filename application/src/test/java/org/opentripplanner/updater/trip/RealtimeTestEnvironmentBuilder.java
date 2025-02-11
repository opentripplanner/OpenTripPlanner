package org.opentripplanner.updater.trip;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.TimetableRepository;

public class RealtimeTestEnvironmentBuilder implements RealtimeTestConstants {

  private final TimetableRepository timetableRepository = new TimetableRepository(
    SITE_REPOSITORY,
    new Deduplicator()
  );

  public RealtimeTestEnvironmentBuilder addTrip(TripInput trip) {
    createTrip(trip);
    timetableRepository.index();
    return this;
  }

  public RealtimeTestEnvironment build() {
    timetableRepository.initTimeZone(TIME_ZONE);
    timetableRepository.addAgency(TimetableRepositoryForTest.AGENCY);

    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(
      SERVICE_ID,
      List.of(SERVICE_DATE.minusDays(1), SERVICE_DATE, SERVICE_DATE.plusDays(1))
    );
    timetableRepository.getServiceCodes().put(SERVICE_ID, 0);
    timetableRepository.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );

    return new RealtimeTestEnvironment(timetableRepository);
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

    timetableRepository.addTripOnServiceDate(tripOnServiceDate);

    if (tripInput.route().getOperator() != null) {
      timetableRepository.addOperators(List.of(tripInput.route().getOperator()));
    }

    var stopTimes = IntStream
      .range(0, tripInput.stops().size())
      .mapToObj(i -> {
        var stop = tripInput.stops().get(i);
        return createStopTime(trip, i, stop.stop(), stop.arrivalTime(), stop.departureTime());
      })
      .toList();

    TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, null);

    final TripPattern pattern = TimetableRepositoryForTest
      .tripPattern(tripInput.id() + "Pattern", tripInput.route())
      .withStopPattern(
        TimetableRepositoryForTest.stopPattern(
          tripInput.stops().stream().map(TripInput.StopCall::stop).toList()
        )
      )
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();

    timetableRepository.addTripPattern(pattern.getId(), pattern);

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
