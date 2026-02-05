package org.opentripplanner.framework.snapshot;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.snapshot.domain.TripUpdate;
import org.opentripplanner.framework.snapshot.domain.timetable.TimetableRepo;
import org.opentripplanner.framework.snapshot.domain.timetable.TripUpdateService;
import org.opentripplanner.framework.snapshot.domain.transfer.NewStopHandler;
import org.opentripplanner.framework.snapshot.event.DomainEventDispatcher;
import org.opentripplanner.framework.snapshot.persistence.snapshot.SnapshotStore;
import org.opentripplanner.framework.snapshot.persistence.snapshot.DefaultStateAccess;
import org.opentripplanner.framework.snapshot.persistence.world.TransitWorldConfig;

public class SnapshotPrototypeTest {

  private static DefaultStateAccess stateAccess;

  @BeforeEach
  public void setup() {
    // The application wiring necessary to make this work. This can be handled by dagger in the
    // future
    var snapshotStore = new SnapshotStore(TransitWorldConfig.provideTransitWorld());
    DomainEventDispatcher dispatcher = new DomainEventDispatcher();
    dispatcher.register(new NewStopHandler());
    dispatcher.freeze();
    stateAccess = new DefaultStateAccess(snapshotStore, dispatcher);
  }

  @Test
  public void readStuff() {
    String tripId = stateAccess.read(snapshot -> snapshot.timetables().getTripId());
    assertThat(tripId).isEqualTo("abc");
  }

  @Test
  public void readMoreStuff() {
    String result = stateAccess.read(snapshot -> {
      String id = snapshot.timetables().getTripId();
      id += "xyz";
      int numberOfRecalculations = snapshot.transfers().getNumberOfRecalculations();
      return id + numberOfRecalculations;
    });
    assertThat(result).isEqualTo("abcxyz0");
  }

  @Test
  public void writeStuff() {
    stateAccess.write(ctx -> ctx.timetable().setTripId("123"));
    String tripId = stateAccess.read(snapshot -> snapshot.timetables().getTripId());
    assertThat(tripId).isEqualTo("123");
  }

  @Test
  public void writeMoreStuff() {
    TripUpdate update = new TripUpdate("newTrip", true);
    stateAccess.write(ctx -> {
      TimetableRepo timetableRepo = ctx.timetable();
      var tripUpdateService = new TripUpdateService(timetableRepo, ctx::publish);
      tripUpdateService.doTripUpdate(update);
    });

    // check that the event was handled
    Integer recalcNum = stateAccess.read(
      snapshot -> snapshot.transfers().getNumberOfRecalculations());
    assertThat(recalcNum).isEqualTo(1);
  }

}