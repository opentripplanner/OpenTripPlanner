package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
public class ScheduledDeviatedTrip extends FlexTrip {
  static final int MISSING_VALUE = -999;

  private final StopLocation[] stops;
  private final int[] departureTimes;
  private final int[] arrivalTimes;

  private final int[] pickupTypes;
  private final int[] dropOffTypes;

  public ScheduledDeviatedTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    int nStops = stopTimes.size();
    this.stops = new StopLocation[nStops];
    this.departureTimes = new int[nStops];
    this.arrivalTimes = new int[nStops];
    this.pickupTypes = new int[nStops];
    this.dropOffTypes = new int[nStops];

    for (int i = 0; i < nStops; i++) {
      StopTime st = stopTimes.get(i);

      this.stops[i] = st.getStop();
      this.arrivalTimes[i] = st.getArrivalTime();
      this.departureTimes[i] = st.getDepartureTime();

      this.pickupTypes[i] = st.getPickupType();
      this.dropOffTypes[i] = st.getDropOffType();
    }
  }

  @Override
  public Collection<StopLocation> getStops() {
    return new HashSet<>(Arrays.asList(stops));
  }

}
