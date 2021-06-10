package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;
import static org.opentripplanner.model.StopTime.MISSING_VALUE;

/**
 * A scheduled deviated trip is similar to a regular scheduled trip, except that is continues stop
 * locations, which are not stops, but other types, such as groups of stops or location areas.
 */
public class ScheduledDeviatedTrip extends FlexTrip {

private static final long serialVersionUID = -8704964150428339679L;

private final ScheduledDeviatedStopTime[] stopTimes;

  private final BookingInfo[] dropOffBookingInfos;
  private final BookingInfo[] pickupBookingInfos;

  public static boolean isScheduledFlexTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> notStopType = Predicate.not(st -> st.getStop() instanceof Stop);
    Predicate<StopTime> notContinuousStop = stopTime ->
        stopTime.getFlexContinuousDropOff() == PICKDROP_NONE && stopTime.getFlexContinuousPickup() == PICKDROP_NONE;
    return stopTimes.stream().anyMatch(notStopType)
        && stopTimes.stream().allMatch(notContinuousStop);
  }

  public ScheduledDeviatedTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    if (!isScheduledFlexTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for scheduled flex trip");
    }

    int nStops = stopTimes.size();
    this.stopTimes = new ScheduledDeviatedStopTime[nStops];
    this.dropOffBookingInfos = new BookingInfo[nStops];
    this.pickupBookingInfos = new BookingInfo[nStops];

    for (int i = 0; i < nStops; i++) {
      this.stopTimes[i] = new ScheduledDeviatedStopTime(stopTimes.get(i));
      this.dropOffBookingInfos[i] = stopTimes.get(i).getDropOffBookingInfo();
      this.pickupBookingInfos[i] = stopTimes.get(i).getPickupBookingInfo();
    }
  }

  @Override
  public Stream<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate serviceDate, FlexPathCalculator calculator
  ) {
    int fromIndex = getFromIndex(access);

    if (fromIndex == -1) { return Stream.empty(); }

    ArrayList<FlexAccessTemplate> res = new ArrayList<>();

    for (int toIndex = fromIndex + 1; toIndex < stopTimes.length; toIndex++) {
      if (stopTimes[toIndex].dropOffType == PICKDROP_NONE) continue;
      for (StopLocation stop : expandStops(stopTimes[toIndex].stop)) {
        res.add(new FlexAccessTemplate(access, this, fromIndex, toIndex, stop, serviceDate, calculator));
      }
    }

    return res.stream();
  }

  @Override
  public Stream<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate serviceDate, FlexPathCalculator calculator
  ) {
    int toIndex = getToIndex(egress);

    if (toIndex == -1) { return Stream.empty(); }

    ArrayList<FlexEgressTemplate> res = new ArrayList<>();

    for (int fromIndex = toIndex - 1; fromIndex >= 0; fromIndex--) {
      if (stopTimes[fromIndex].pickupType == PICKDROP_NONE) continue;
      for (StopLocation stop : expandStops(stopTimes[fromIndex].stop)) {
        res.add(new FlexEgressTemplate(egress, this, fromIndex, toIndex, stop, serviceDate, calculator));
      }
    }

    return res.stream();
  }

  @Override
  public int earliestDepartureTime(
      int departureTime, int fromStopIndex, int toStopIndex
  ) {
    int stopTime = MISSING_VALUE;
    for (int i = fromStopIndex; stopTime == MISSING_VALUE && i >= 0; i--) {
      stopTime = stopTimes[i].departureTime;
    }
    return stopTime != MISSING_VALUE && stopTime >= departureTime ? stopTime : -1;
  }

  @Override
  public int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex) {
    int stopTime = MISSING_VALUE;
    for (int i = toStopIndex; stopTime == MISSING_VALUE && i < stopTimes.length; i++) {
      stopTime = stopTimes[i].arrivalTime;
    }
    return stopTime != MISSING_VALUE && stopTime <= arrivalTime ? stopTime : -1;
  }

  @Override
  public Collection<StopLocation> getStops() {
    return Arrays
        .stream(stopTimes)
        .map(scheduledDeviatedStopTime -> scheduledDeviatedStopTime.stop)
        .collect(Collectors.toSet());
  }

  @Override
  public BookingInfo getDropOffBookingInfo(int i) {
    return dropOffBookingInfos[i];
  }

  @Override
  public BookingInfo getPickupBookingInfo(int i) {
    return pickupBookingInfos[i];
  }

  private Collection<StopLocation> expandStops(StopLocation stop) {
    return stop instanceof FlexLocationGroup
        ? ((FlexLocationGroup) stop).getLocations()
        : Collections.singleton(stop);
  }

  private int getFromIndex(NearbyStop accessEgress) {
    for (int i = 0; i < stopTimes.length; i++) {
      if (stopTimes[i].pickupType == PICKDROP_NONE) continue; // No pickup allowed here
      StopLocation stop = stopTimes[i].stop;
      if (stop instanceof FlexLocationGroup) {
        if (((FlexLocationGroup) stop).getLocations().contains(accessEgress.stop)) {
          return i;
        }
      }
      else {
        if (stop.equals(accessEgress.stop)) {
          return i;
        }
      }
    }
    return -1;
  }

  private int getToIndex(NearbyStop accessEgress) {
    for (int i = stopTimes.length - 1; i >= 0; i--) {
      if (stopTimes[i].dropOffType == PICKDROP_NONE) continue; // No drop off allowed here
      StopLocation stop = stopTimes[i].stop;
      if (stop instanceof FlexLocationGroup) {
        if (((FlexLocationGroup) stop).getLocations().contains(accessEgress.stop)) {
          return i;
        }
      }
      else {
        if (stop.equals(accessEgress.stop)) {
          return i;
        }
      }
    }
    return -1;
  }

  @Override
  public int getSafeTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex) {
		ScheduledDeviatedStopTime fromStopTime = this.stopTimes[fromStopIndex];
		ScheduledDeviatedStopTime toStopTime = this.stopTimes[toStopIndex];
		
		if(fromStopTime.stop.isArea() || toStopTime.stop.isArea()) {
			int safeFactor = Math.max(fromStopTime.safeFactor, toStopTime.safeFactor);
			int safeOffset = Math.max(fromStopTime.safeOffset, toStopTime.safeOffset);
			if(safeFactor != MISSING_VALUE && safeOffset != MISSING_VALUE)
				return (safeFactor * streetPath.durationSeconds) + safeOffset;
		}
		return streetPath.durationSeconds;					
  }

  @Override
  public int getMeanTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex) {
		ScheduledDeviatedStopTime fromStopTime = this.stopTimes[fromStopIndex];
		ScheduledDeviatedStopTime toStopTime = this.stopTimes[toStopIndex];
		
		if(fromStopTime.stop.isArea() || toStopTime.stop.isArea()) {
			int meanFactor = Math.max(fromStopTime.meanFactor, toStopTime.meanFactor);
			int meanOffset = Math.max(fromStopTime.meanOffset, toStopTime.meanOffset);
			if(meanFactor != MISSING_VALUE && meanOffset != MISSING_VALUE)
				return (meanFactor * streetPath.durationSeconds) + meanOffset;
		}
		return streetPath.durationSeconds;
  }

  private static class ScheduledDeviatedStopTime implements Serializable {
	private static final long serialVersionUID = 7176753358641800732L;
	private final StopLocation stop;
    private final int departureTime;
    private final int arrivalTime;
    private final int pickupType;
    private final int dropOffType;
    private final int safeFactor;
    private final int safeOffset;
    private final int meanFactor;
    private final int meanOffset;
    
    private ScheduledDeviatedStopTime(StopTime st) {
      this.stop = st.getStop();

      this.safeFactor = st.getSafeDurationFactor();
      this.safeOffset = st.getSafeDurationOffset();

      this.meanFactor = st.getMeanDurationFactor();
      this.meanOffset = st.getMeanDurationOffset();

      // Store the time the user is guaranteed to arrive at latest
      this.arrivalTime = st.getFlexWindowEnd() != MISSING_VALUE ? st.getFlexWindowEnd() : st.getArrivalTime();
      
      // Store the time the user needs to be ready for pickup
      this.departureTime = st.getFlexWindowStart() != MISSING_VALUE ? st.getFlexWindowStart() : st.getDepartureTime();

      // TODO: Store the window for a stop, and allow the user to have an "unguaranteed"
      // pickup/dropoff between the start and end of the window
      this.pickupType = st.getPickupType();
      this.dropOffType = st.getDropOffType();
    }
  }

}
