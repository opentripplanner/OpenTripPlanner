package org.opentripplanner.transit.model.timetable;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Value Object containing Metadata about the realtime state of a Trip,
 * such as the State of the Realtime Information at each stop, Vehicle occupancy etc.
 * Used to encapsulate mostly API Relevant information.
 */
public class TripRealTimeMetadata implements Serializable {

  private final RealTimeState realTimeState;
  private final StopRealTimeState[] stopRealTimeStates;
  private final OccupancyStatus[] occupancyStatuses;

  @Nullable
  private final ZonedDateTime lastUpdated;

  private TripRealTimeMetadata(
    RealTimeState realTimeState,
    StopRealTimeState[] stopRealTimeStates,
    OccupancyStatus[] occupancyStatuses,
    ZonedDateTime lastUpdated
  ) {
    this.realTimeState = realTimeState;
    this.stopRealTimeStates = stopRealTimeStates;
    this.lastUpdated = lastUpdated;
    this.occupancyStatuses = occupancyStatuses;
  }

  private TripRealTimeMetadata(int numStops) {
    this(
      RealTimeState.SCHEDULED,
      defaultStopStates(numStops),
      defaultOccupancyStatuses(numStops),
      null
    );
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TripRealTimeMetadata that = (TripRealTimeMetadata) o;
    return (
      realTimeState == that.realTimeState &&
      Objects.deepEquals(stopRealTimeStates, that.stopRealTimeStates) &&
      Objects.equals(lastUpdated, that.lastUpdated) &&
      Objects.deepEquals(occupancyStatuses, that.occupancyStatuses)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      realTimeState,
      Arrays.hashCode(stopRealTimeStates),
      lastUpdated,
      Arrays.hashCode(occupancyStatuses)
    );
  }

  public StopRealTimeState getRealTimeStateForStop(int index) {
    return stopRealTimeStates[index];
  }

  public OccupancyStatus getOccupancyStatusForStop(int index) {
    return occupancyStatuses[index];
  }

  public RealTimeState realTimeState() {
    return realTimeState;
  }

  public ZonedDateTime lastUpdated() {
    return lastUpdated;
  }

  /**
   * The real-time states for a given stops. If the state is DEFAULT for a stop, the
   * {@link #realTimeState()} should determine the realtime state of the stop.
   * <p>
   * This is only for API-purposes (does not affect routing).
   */
  public boolean hasStopRealTimeState(int stopPos, StopRealTimeState state) {
    return stopRealTimeStates[stopPos] == state;
  }

  public static TripRealTimeMetadataBuilder builder(int numStops) {
    return new TripRealTimeMetadataBuilder(numStops);
  }

  public static TripRealTimeMetadata defaultRealTimeMetadata(int numStops) {
    return new TripRealTimeMetadata(numStops);
  }

  private static StopRealTimeState[] defaultStopStates(int numStops) {
    StopRealTimeState[] states = new StopRealTimeState[numStops];
    Arrays.fill(states, StopRealTimeState.DEFAULT);
    return states;
  }

  private static OccupancyStatus[] defaultOccupancyStatuses(int numStops) {
    OccupancyStatus[] statuses = new OccupancyStatus[numStops];
    Arrays.fill(statuses, OccupancyStatus.NO_DATA_AVAILABLE);
    return statuses;
  }

  public static class TripRealTimeMetadataBuilder {

    private boolean receivedTimeUpdates = false;
    private RealTimeState realTimeState;
    private final StopRealTimeState[] stopRealTimeStates;
    private ZonedDateTime lastUpdated;
    private final OccupancyStatus[] occupancyStatuses;

    private TripRealTimeMetadataBuilder(int numStops) {
      stopRealTimeStates = new StopRealTimeState[numStops];
      occupancyStatuses = new OccupancyStatus[numStops];
      Arrays.fill(stopRealTimeStates, StopRealTimeState.DEFAULT);
      Arrays.fill(occupancyStatuses, OccupancyStatus.NO_DATA_AVAILABLE);
    }

    public TripRealTimeMetadataBuilder withRealTimeState(RealTimeState realTimeState) {
      this.realTimeState = realTimeState;
      return this;
    }

    public TripRealTimeMetadataBuilder withRealTimeStateAtStop(
      int index,
      StopRealTimeState stopRealTimeState
    ) {
      stopRealTimeStates[index] = stopRealTimeState;
      if (stopRealTimeState != StopRealTimeState.NO_DATA) {
        receivedTimeUpdates = true;
      }
      return this;
    }

    public TripRealTimeMetadataBuilder withLastUpdated(ZonedDateTime lastUpdated) {
      this.lastUpdated = lastUpdated;
      return this;
    }

    public TripRealTimeMetadataBuilder withOccupancyAtStop(
      int index,
      OccupancyStatus occupancyStatus
    ) {
      occupancyStatuses[index] = occupancyStatus;
      return this;
    }

    public TripRealTimeMetadataBuilder cancelTrip() {
      return withRealTimeState(RealTimeState.CANCELED);
    }

    public TripRealTimeMetadataBuilder deleteTrip() {
      return withRealTimeState(RealTimeState.DELETED);
    }

    public TripRealTimeMetadataBuilder withInaccuratePredictions(int stop) {
      return withRealTimeStateAtStop(stop, StopRealTimeState.INACCURATE_PREDICTIONS);
    }

    public TripRealTimeMetadataBuilder withCanceled(int stop) {
      return withRealTimeStateAtStop(stop, StopRealTimeState.CANCELLED);
    }

    public TripRealTimeMetadataBuilder withNoData(int stop) {
      return withRealTimeStateAtStop(stop, StopRealTimeState.NO_DATA);
    }

    public StopRealTimeState getStopRealTimeState(int stop) {
      return stopRealTimeStates[stop];
    }

    public TripRealTimeMetadata build() {
      RealTimeState state = realTimeState == null
        ? (receivedTimeUpdates ? RealTimeState.UPDATED : RealTimeState.SCHEDULED)
        : realTimeState;
      return new TripRealTimeMetadata(state, stopRealTimeStates, occupancyStatuses, lastUpdated);
    }
  }
}
