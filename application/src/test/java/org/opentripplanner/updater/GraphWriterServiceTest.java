package org.opentripplanner.updater;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import com.google.common.collect.ArrayListMultimap;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
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
 * Tests the transit-data views that {@link GraphWriterService} hands to update tasks through the
 * {@link TransitRealTimeUpdateContext}: the uncommitted view ({@code transitService()}) sees
 * changes in the write buffer, the committed view ({@code committedTransitService()}) sees only
 * the last committed snapshot, and resolving the committed view does not cause a new timetable
 * snapshot to be published at commit.
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
  void committedTransitServiceDoesNotSeeUncommittedChanges() throws Exception {
    var uncommittedView = new AtomicReference<TripPattern>();
    var committedView = new AtomicReference<TripPattern>();

    service
      .execute(context -> {
        context.timetableRepository().update(modifiedPatternUpdate());
        uncommittedView.set(context.transitService().findPattern(trip, env.defaultServiceDate()));
        committedView.set(
          context.committedTransitService().findPattern(trip, env.defaultServiceDate())
        );
      })
      .get();

    assertThat(uncommittedView.get()).isSameInstanceAs(modifiedPattern);
    assertThat(committedView.get()).isSameInstanceAs(scheduledPattern);
  }

  @Test
  void committedTransitServiceSeesChangesOfCommittedTransactions() throws Exception {
    service.execute(context -> context.timetableRepository().update(modifiedPatternUpdate())).get();

    var committedView = new AtomicReference<TripPattern>();
    service
      .execute(context ->
        committedView.set(
          context.committedTransitService().findPattern(trip, env.defaultServiceDate())
        )
      )
      .get();

    assertThat(committedView.get()).isSameInstanceAs(modifiedPattern);
  }

  @Test
  void vehicleOnlyTaskDoesNotFreezeTheTimetableSnapshot() throws Exception {
    var timetableSnapshotBefore = env.timetableSnapshot();
    var vehicleSnapshotBefore = vehicleHandle.repositorySnapshot(env.repositoryRegistry().scope());

    service
      .execute(context -> {
        // Reading the committed view must not cause a new timetable snapshot to be published.
        context.committedTransitService().findPattern(trip, env.defaultServiceDate());
        context
          .realtimeVehicleRepository()
          .setRealtimeVehiclesForFeed(env.feedId(), ArrayListMultimap.create());
      })
      .get();

    var timetableSnapshotAfter = env.timetableSnapshot();
    var vehicleSnapshotAfter = vehicleHandle.repositorySnapshot(env.repositoryRegistry().scope());

    assertThat(timetableSnapshotAfter).isSameInstanceAs(timetableSnapshotBefore);
    assertThat(vehicleSnapshotAfter).isNotSameInstanceAs(vehicleSnapshotBefore);
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
