package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class UnscheduledTrip extends FlexTrip {

  private final StopLocation[] stops;
  private final int[] minDepartureTimes;
  private final int[] maxDepartureTimes;
  private final int[] minArrivalTimes;
  private final int[] maxArrivalTimes;

  private final int[] pickupTypes;
  private final int[] dropOffTypes;


  public UnscheduledTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    int nStops = stopTimes.size();
    this.stops = new StopLocation[nStops];
    this.minDepartureTimes = new int[nStops];
    this.maxDepartureTimes = new int[nStops];
    this.minArrivalTimes = new int[nStops];
    this.maxArrivalTimes = new int[nStops];
    this.pickupTypes = new int[nStops];
    this.dropOffTypes = new int[nStops];

    for (int i = 0; i < nStops; i++) {
      StopTime st = stopTimes.get(i);

      this.stops[i] = st.getStop();
      this.minArrivalTimes[i] = st.getMinArrivalTime();
      this.minDepartureTimes[i] = st.getMinArrivalTime(); //TODO
      this.maxArrivalTimes[i] = st.getMaxDepartureTime(); //TODO
      this.maxDepartureTimes[i] = st.getMaxDepartureTime();

      this.pickupTypes[i] = st.getPickupType();
      this.dropOffTypes[i] = st.getDropOffType();
    }
  }

  @Override
  public Collection<StopLocation> getStops() {
    return new HashSet<>(Arrays.asList(stops));
  }

}
