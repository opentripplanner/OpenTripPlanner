package org.opentripplanner.updater;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import com.google.common.collect.ArrayListMultimap;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepositorySnapshot;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.internal.RealtimeVehicleRepositoryLifecycle;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Tests the timetable views that {@link GraphWriterService} hands to the update tasks of the
 * transit write domain.
 */
class GraphWriterServiceTest {

  private static final String TRIP_ID = "trip1";

  private TransitTestEnvironment env;
  private RepositoryHandle<
    RealtimeVehicleRepositorySnapshot,
    RealtimeVehicleRepository
  > vehicleHandle;
  private GraphWriterService<TransitRealTimeUpdateContext> service;
  private Trip trip;
  private TripPattern scheduledPattern;
  private TripPattern modifiedPattern;

  @BeforeEach
  void setUp() {
    var envBuilder = TransitTestEnvironment.of();
    RegularStop stop1 = envBuilder.stop("S1");
    RegularStop stop2 = envBuilder.stop("S2");
    RegularStop stop3 = envBuilder.stop("S3");
    env = envBuilder
      .addTrip(TripInput.of(TRIP_ID).addStop(stop1, "00:00").addStop(stop2, "00:01"))
      .build();

    vehicleHandle = env
      .repositoryRegistry()
      .registerRepository(
        new DefaultRealtimeVehicleRepository(),
        new RealtimeVehicleRepositoryLifecycle()
      );
    service = GraphWriterService.forTransitDomain(
      env.updateManager(),
      env.repositoryRegistry(),
      env.timetableHandle(),
      vehicleHandle,
      env.transitRepository()
    );

    trip = env.tripData(TRIP_ID).trip();
    scheduledPattern = env.tripData(TRIP_ID).scheduledTripPattern();
    modifiedPattern = TripPattern.of(id("modified"))
      .withRoute(scheduledPattern.getRoute())
      .withStopPattern(stopPattern(stop1, stop3))
      .withRealTimeStopPatternModified()
      .build();
  }

  @AfterEach
  void tearDown() {
    service.stop();
  }

  @Test
  void committedSnapshotDoesNotSeeUncommittedChanges() throws Exception {
    var uncommittedView = new AtomicReference<TripPattern>();
    var committedView = new AtomicReference<TripPattern>();

    service
      .execute(context -> {
        context.timetableRepository().update(modifiedPatternUpdate());
        uncommittedView.set(
          context.timetableRepository().findPattern(trip, env.defaultServiceDate())
        );
        committedView.set(
          context.committedTimetableSnapshot().findPattern(trip, env.defaultServiceDate())
        );
      })
      .get();

    assertSame(modifiedPattern, uncommittedView.get());
    assertSame(scheduledPattern, committedView.get());
  }

  @Test
  void committedSnapshotSeesChangesOfCommittedTransactions() throws Exception {
    service.execute(context -> context.timetableRepository().update(modifiedPatternUpdate())).get();

    var committedView = new AtomicReference<TripPattern>();
    service
      .execute(context ->
        committedView.set(
          context.committedTimetableSnapshot().findPattern(trip, env.defaultServiceDate())
        )
      )
      .get();

    assertSame(modifiedPattern, committedView.get());
  }

  @Test
  void vehicleOnlyTaskDoesNotPublishNewTimetableSnapshot() throws Exception {
    var timetableSnapshotBefore = env.timetableSnapshot();
    var vehicleSnapshotBefore = vehicleSnapshot();

    service
      .execute(context -> {
        context.committedTimetableSnapshot().findPattern(trip, env.defaultServiceDate());
        context
          .realtimeVehicleRepository()
          .setRealtimeVehiclesForFeed(env.feedId(), ArrayListMultimap.create());
      })
      .get();

    assertSame(timetableSnapshotBefore, env.timetableSnapshot());
    assertNotSame(vehicleSnapshotBefore, vehicleSnapshot());
  }

  @Test
  void timetableOnlyTaskDoesNotPublishNewVehicleSnapshot() throws Exception {
    var timetableSnapshotBefore = env.timetableSnapshot();
    var vehicleSnapshotBefore = vehicleSnapshot();

    service.execute(context -> context.timetableRepository().update(modifiedPatternUpdate())).get();

    assertNotSame(timetableSnapshotBefore, env.timetableSnapshot());
    assertSame(vehicleSnapshotBefore, vehicleSnapshot());
  }

  private RealtimeVehicleRepositorySnapshot vehicleSnapshot() {
    return vehicleHandle.repositorySnapshot(env.repositoryRegistry().scope());
  }

  private RealTimeTripUpdate modifiedPatternUpdate() {
    var realTimeTripTimes = env
      .tripData(TRIP_ID)
      .scheduledTripTimes()
      .createRealTimeFromScheduledTimes()
      .build();
    return RealTimeTripUpdate.of(
      modifiedPattern,
      realTimeTripTimes,
      env.defaultServiceDate()
    ).build();
  }

  private static StopPattern stopPattern(RegularStop... stops) {
    var builder = StopPattern.create(stops.length);
    for (int i = 0; i < stops.length; i++) {
      builder.stops.with(i, stops[i]);
      builder.pickups.with(i, PickDrop.SCHEDULED);
      builder.dropoffs.with(i, PickDrop.SCHEDULED);
    }
    return builder.build();
  }
}
