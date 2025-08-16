package org.opentripplanner.ext.carpooling.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.ZonedDateTime;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.timetable.Trip;

class DefaultCarpoolingRepositoryTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final AreaStop PICKUP_AREA = TEST_MODEL.areaStop("pickup-area-1").build();
  private static final AreaStop DROPOFF_AREA = TEST_MODEL.areaStop("dropoff-area-1").build();
  private static final Trip TEST_TRIP = TEST_MODEL.trip("carpool-trip").build();

  private static final FeedScopedId TRIP_ID_1 = id("carpool-trip-1");
  private static final FeedScopedId TRIP_ID_2 = id("carpool-trip-2");

  private CarpoolingRepository repository;
  private CarpoolTrip carpoolTrip1;
  private CarpoolTrip carpoolTrip2;

  @BeforeEach
  void setup() {
    repository = new DefaultCarpoolingRepository(null);

    carpoolTrip1 = new CarpoolTripBuilder(TRIP_ID_1)
      .withBoardingArea(PICKUP_AREA)
      .withAlightingArea(DROPOFF_AREA)
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withTrip(TEST_TRIP)
      .withProvider("TestProvider")
      .withAvailableSeats(3)
      .build();

    carpoolTrip2 = new CarpoolTripBuilder(TRIP_ID_2)
      .withBoardingArea(PICKUP_AREA)
      .withAlightingArea(DROPOFF_AREA)
      .withStartTime(ZonedDateTime.now().plusHours(1))
      .withEndTime(ZonedDateTime.now().plusHours(1).plusMinutes(30))
      .withTrip(TEST_TRIP)
      .withProvider("TestProvider2")
      .withAvailableSeats(2)
      .build();
  }

  @Test
  void getCarpoolTrips_emptyRepository() {
    Collection<CarpoolTrip> trips = repository.getCarpoolTrips();
    assertTrue(trips.isEmpty());
  }

  @Test
  void addCarpoolTrip_singleTrip() {
    repository.addCarpoolTrip(carpoolTrip1);

    Collection<CarpoolTrip> trips = repository.getCarpoolTrips();
    assertEquals(1, trips.size());
    assertTrue(trips.contains(carpoolTrip1));
  }

  @Test
  void addCarpoolTrip_multipleTrips() {
    repository.addCarpoolTrip(carpoolTrip1);
    repository.addCarpoolTrip(carpoolTrip2);

    Collection<CarpoolTrip> trips = repository.getCarpoolTrips();
    assertEquals(2, trips.size());
    assertTrue(trips.contains(carpoolTrip1));
    assertTrue(trips.contains(carpoolTrip2));
  }

  @Test
  void getCarpoolTrip_existingTrip() {
    repository.addCarpoolTrip(carpoolTrip1);

    CarpoolTrip retrievedTrip = repository.getCarpoolTrip(TRIP_ID_1);
    assertEquals(carpoolTrip1, retrievedTrip);
  }

  @Test
  void getCarpoolTrip_nonExistentTrip() {
    CarpoolTrip retrievedTrip = repository.getCarpoolTrip(TRIP_ID_1);
    assertNull(retrievedTrip);
  }

  @Test
  void removeCarpoolTrip_existingTrip() {
    repository.addCarpoolTrip(carpoolTrip1);
    repository.addCarpoolTrip(carpoolTrip2);

    assertEquals(2, repository.getCarpoolTrips().size());

    repository.removeCarpoolTrip(TRIP_ID_1);

    Collection<CarpoolTrip> trips = repository.getCarpoolTrips();
    assertEquals(1, trips.size());
    assertTrue(trips.contains(carpoolTrip2));
    assertNull(repository.getCarpoolTrip(TRIP_ID_1));
  }

  @Test
  void removeCarpoolTrip_nonExistentTrip() {
    repository.addCarpoolTrip(carpoolTrip1);

    // Should not throw an exception when removing non-existent trip
    repository.removeCarpoolTrip(TRIP_ID_2);

    assertEquals(1, repository.getCarpoolTrips().size());
    assertTrue(repository.getCarpoolTrips().contains(carpoolTrip1));
  }

  @Test
  void addCarpoolTrip_replaceExistingTrip() {
    repository.addCarpoolTrip(carpoolTrip1);

    // Create a new trip with the same ID but different details
    CarpoolTrip replacementTrip = new CarpoolTripBuilder(TRIP_ID_1)
      .withBoardingArea(DROPOFF_AREA) // Different pickup area
      .withAlightingArea(PICKUP_AREA) // Different dropoff area
      .withStartTime(ZonedDateTime.now().plusHours(2))
      .withEndTime(ZonedDateTime.now().plusHours(2).plusMinutes(45))
      .withTrip(TEST_TRIP)
      .withProvider("ReplacementProvider")
      .withAvailableSeats(1)
      .build();

    repository.addCarpoolTrip(replacementTrip);

    Collection<CarpoolTrip> trips = repository.getCarpoolTrips();
    assertEquals(1, trips.size());

    CarpoolTrip retrievedTrip = repository.getCarpoolTrip(TRIP_ID_1);
    assertEquals(replacementTrip, retrievedTrip);
    assertEquals("ReplacementProvider", retrievedTrip.getProvider());
    assertEquals(1, retrievedTrip.getAvailableSeats());
  }

  @Test
  void concurrentAccess_threadSafety() throws InterruptedException {
    // Test thread safety by adding trips from multiple threads
    Thread thread1 = new Thread(() -> {
      for (int i = 0; i < 100; i++) {
        FeedScopedId id = new FeedScopedId("test", "trip-thread1-" + i);
        CarpoolTrip trip = new CarpoolTripBuilder(id)
          .withBoardingArea(PICKUP_AREA)
          .withAlightingArea(DROPOFF_AREA)
          .withStartTime(ZonedDateTime.now())
          .withEndTime(ZonedDateTime.now().plusMinutes(30))
          .withTrip(TEST_TRIP)
          .withProvider("Thread1Provider")
          .withAvailableSeats(1)
          .build();
        repository.addCarpoolTrip(trip);
      }
    });

    Thread thread2 = new Thread(() -> {
      for (int i = 0; i < 100; i++) {
        FeedScopedId id = new FeedScopedId("test", "trip-thread2-" + i);
        CarpoolTrip trip = new CarpoolTripBuilder(id)
          .withBoardingArea(PICKUP_AREA)
          .withAlightingArea(DROPOFF_AREA)
          .withStartTime(ZonedDateTime.now())
          .withEndTime(ZonedDateTime.now().plusMinutes(30))
          .withTrip(TEST_TRIP)
          .withProvider("Thread2Provider")
          .withAvailableSeats(2)
          .build();
        repository.addCarpoolTrip(trip);
      }
    });

    thread1.start();
    thread2.start();
    thread1.join();
    thread2.join();

    assertEquals(200, repository.getCarpoolTrips().size());
  }
}
