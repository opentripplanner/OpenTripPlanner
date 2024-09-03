package org.opentripplanner.updater.trip;

import com.esotericsoftware.kryo.util.Null;
import com.google.transit.realtime.GtfsRealtime;
import de.mfdz.MfdzRealtimeExtensions;
import gnu.trove.set.hash.TIntHashSet;
import io.grpc.netty.shaded.io.netty.util.concurrent.ProgressivePromise;
import java.sql.Time;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.opentripplanner.gtfs.mapping.PickDropMapper;
import org.opentripplanner.model.PickDrop;

/**
 * This class purely exists to encapsulate the logic for extracting conversion of the GTFS-RT
 * updates into a separate place.
 */
final class AddedStopTime {

  @Nullable
  private final PickDrop pickup;

  @Nullable
  private final PickDrop dropOff;

  @Nullable
  public final String stopId;

  @Nullable
  private final GtfsRealtime.TripUpdate.StopTimeEvent arrival;

  @Nullable
  private final GtfsRealtime.TripUpdate.StopTimeEvent departure;

  @Nullable
  public final Integer stopSequence;

  public final GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship scheduleRelationship;

  public static final PickDrop DEFAULT_PICK_DROP = PickDrop.SCHEDULED;

  AddedStopTime(
    @Nullable PickDrop pickup,
    @Nullable PickDrop dropOff,
    @Nullable String stopId,
    @Nullable GtfsRealtime.TripUpdate.StopTimeEvent arrival,
    @Nullable GtfsRealtime.TripUpdate.StopTimeEvent departure,
    @Nullable Integer stopSequence,
    @Nullable GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship scheduleRelationship
  ) {
    this.pickup = pickup;
    this.dropOff = dropOff;
    this.stopId = stopId;
    this.arrival = arrival;
    this.departure = departure;
    this.stopSequence = stopSequence;
    this.scheduleRelationship = scheduleRelationship;
  }

  PickDrop pickup() {
    return Objects.requireNonNullElse(pickup, DEFAULT_PICK_DROP);
  }

  PickDrop dropOff() {
    return Objects.requireNonNullElse(dropOff, DEFAULT_PICK_DROP);
  }

  @Nullable
  Long getArrivalTime() {
    if (arrival == null) {
      return null;
    }
    return arrival.hasTime() ? arrival.getTime() : null;
  }

  @Nullable
  Long getDepartureTime() {
    if (departure == null) {
      return null;
    }
    return departure.hasTime() ? departure.getTime() : null;
  }

  @Nullable
  Integer getArrivalDelay() {
    if (arrival == null) {
      return null;
    }
    return arrival.hasDelay() ? arrival.getDelay() : null;
  }

  @Nullable
  Integer getDepartureDelay() {
    if (departure == null) {
      return null;
    }
    return departure.hasDelay() ? departure.getDelay() : null;
  }

  static AddedStopTime ofStopTime(GtfsRealtime.TripUpdate.StopTimeUpdate props) {
    final var scheduleRelationship = props.getScheduleRelationship();
    var pickupType = toPickDrop(scheduleRelationship);
    var dropOffType = pickupType;
    if (
      scheduleRelationship != GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED &&
      props.getStopTimeProperties().hasExtension(MfdzRealtimeExtensions.stopTimeProperties)
    ) {
      var ext = props
        .getStopTimeProperties()
        .getExtension(MfdzRealtimeExtensions.stopTimeProperties);
      var pickup = ext.getPickupType();
      var dropOff = ext.getDropoffType();
      dropOffType = PickDropMapper.map(dropOff.getNumber());
      pickupType = PickDropMapper.map(pickup.getNumber());
    }
    return new AddedStopTime(
      pickupType,
      dropOffType,
      props.hasStopId() ? props.getStopId() : null,
      props.hasArrival() ? props.getArrival() : null,
      props.hasDeparture() ? props.getDeparture() : null,
      props.hasStopSequence() ? props.getStopSequence() : null,
      scheduleRelationship
    );
  }

  private static PickDrop toPickDrop(
    GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship scheduleRelationship
  ) {
    return switch (scheduleRelationship) {
      case SCHEDULED, NO_DATA, UNSCHEDULED -> PickDrop.SCHEDULED;
      case SKIPPED -> PickDrop.CANCELLED;
    };
  }
}
