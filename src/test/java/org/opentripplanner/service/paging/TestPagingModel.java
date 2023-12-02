package org.opentripplanner.service.paging;

import static org.opentripplanner.framework.time.TimeUtils.hm2time;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_ARRIVAL_TIME;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.routing.algorithm.filterchain.comparator.SortOrderComparator;
import org.opentripplanner.transit.model._data.TransitModelForTest;

class TestPagingModel {

  static final int T12_00 = hm2time(12, 0);
  static final int T12_09 = hm2time(12, 9);
  static final int T12_10 = hm2time(12, 10);
  static final int T12_25 = hm2time(12, 25);
  static final int T12_29 = hm2time(12, 29);
  static final int T12_30 = hm2time(12, 30);
  static final int T12_39 = hm2time(12, 39);
  static final int T12_40 = hm2time(12, 40);
  static final int T12_41 = hm2time(12, 41);
  static final int T12_55 = hm2time(12, 55);
  static final int T13_00 = hm2time(13, 0);
  static final int T13_10 = hm2time(13, 10);
  static final int T13_11 = hm2time(13, 11);
  static final int T13_25 = hm2time(13, 25);
  static final int T13_30 = hm2time(13, 30);

  public static final Duration D10m = Duration.ofMinutes(10);

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();
  private static final Place A = Place.forStop(TEST_MODEL.stop("A").build());
  private static final Place B = Place.forStop(TEST_MODEL.stop("B").build());

  private static final Instant TRANSIT_START_TIME = TestItineraryBuilder.newTime(0).toInstant();
  public static final boolean ON_STREET = false;
  public static final boolean TRANSIT = true;
  public static final int COST_HIGH = 10;
  public static final int COST_LOW = 9;
  public static final int TX_1 = 1;
  public static final int TX_0 = 0;

  private static final List<Itinerary> ITINERARIES = List.of(
    // EDT time-shifted onStreet result (apply to first depart-after search)
    itinerary(T12_00, T12_30, COST_HIGH, 0, ON_STREET),
    // Next  itineraries are almost the same - a criterion is better for each
    itinerary(T12_10, T12_41, COST_HIGH, TX_1, TRANSIT),
    itinerary(T12_10, T12_40, COST_LOW, TX_1, TRANSIT),
    itinerary(T12_10, T12_40, COST_HIGH, TX_0, TRANSIT),
    itinerary(T12_09, T12_40, COST_HIGH, TX_1, TRANSIT),
    itinerary(T12_25, T12_55, COST_HIGH, TX_1, TRANSIT),
    // An itinerary with a very long duration
    itinerary(T12_29, T13_11, COST_LOW, TX_0, TRANSIT),
    // Next  itineraries are almost the same - a criterion is better for each
    itinerary(T12_40, T13_11, COST_HIGH, TX_1, TRANSIT),
    itinerary(T12_40, T13_10, COST_LOW, TX_1, TRANSIT),
    itinerary(T12_40, T13_10, COST_HIGH, TX_0, TRANSIT),
    itinerary(T12_39, T13_10, COST_HIGH, TX_1, TRANSIT),
    itinerary(T12_55, T13_25, COST_HIGH, TX_1, TRANSIT),
    // LAT time-shifted onStreet result (apply to first arrive-by search)
    itinerary(T13_00, T13_30, COST_HIGH, 0, ON_STREET)
  );
  static final List<Itinerary> ITINERARIES_DEPART_AFTER = ITINERARIES
    // Skip last itinerary (onStreet arriveBy)
    .subList(0, ITINERARIES.size() - 1)
    .stream()
    .sorted(SortOrderComparator.comparator(STREET_AND_ARRIVAL_TIME))
    .toList();

  static final List<Itinerary> ITINERARIES_ARRIVE_BY = ITINERARIES
    // Skip first itinerary (onStreet departAfter)
    .subList(1, ITINERARIES.size())
    .stream()
    .sorted(SortOrderComparator.comparator(SortOrder.STREET_AND_DEPARTURE_TIME))
    .toList();

  static final boolean ARRIVE_BY = true;
  static final boolean DEPART_AFTER = false;

  static TestDriver arriveByDriver(int edt, int lat, Duration searchWindow, int nResults) {
    return TestDriver.driver(
      edt,
      lat,
      searchWindow,
      nResults,
      STREET_AND_DEPARTURE_TIME,
      ITINERARIES_ARRIVE_BY
    );
  }

  static TestDriver departAfterDriver(int edt, Duration searchWindow, int nResults) {
    return TestDriver.driver(
      edt,
      -1,
      searchWindow,
      nResults,
      STREET_AND_ARRIVAL_TIME,
      ITINERARIES_DEPART_AFTER
    );
  }

  private static Itinerary itinerary(
    int departureTime,
    int arrivalTime,
    int cost,
    int nTransfers,
    boolean transit
  ) {
    var builder = TestItineraryBuilder.newItinerary(A);

    if (transit) {
      if (nTransfers == 0) {
        builder.bus(10, departureTime, arrivalTime, B);
      } else if (nTransfers == 1) {
        builder
          .bus(20, departureTime, departureTime + 120, B)
          .bus(21, departureTime + 240, arrivalTime, B);
      } else {
        throw new IllegalArgumentException("nTransfers not supported: " + nTransfers);
      }
    } else {
      builder.drive(departureTime, arrivalTime, B);
    }
    var it = builder.build();
    it.setGeneralizedCost(cost);
    return it;
  }

  public static Instant time(int time) {
    return time < 0 ? null : TRANSIT_START_TIME.plusSeconds(time);
  }
}
