package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.Stop;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransitLayer {

  /**
   * Transit data required for routing
   */
  private final HashMap<LocalDate, List<TripPatternForDate>> tripPatternsForDate;

  /**
   * Index of outer list is from stop index, inner list index has no specific meaning. To stop index
   * is a field of the Transfer object.
   */
  private final List<List<Transfer>> transferByStopIndex;

  /**
   * Maps to original graph to retrieve additional data
   */
  private final StopIndexForRaptor stopIndex;

  private final ZoneId transitDataZoneId;

  /**
   * Makes a shallow copy of the TransitLayer, except for the tripPatternsForDate, where a shallow
   * copy of the HashMap is made. This is sufficient, as the TransitLayerUpdater will replace
   * entire keys and their values in the map.
   */
  public TransitLayer(TransitLayer transitLayer) {
    this(
        transitLayer.tripPatternsForDate,
        transitLayer.transferByStopIndex,
        transitLayer.stopIndex,
        transitLayer.transitDataZoneId
    );
  }

  public TransitLayer(
      Map<LocalDate, List<TripPatternForDate>> tripPatternsForDate,
      List<List<Transfer>> transferByStopIndex,
      StopIndexForRaptor stopIndex,
      ZoneId transitDataZoneId
  ) {
    this.tripPatternsForDate = new HashMap<>(tripPatternsForDate);
    this.transferByStopIndex = transferByStopIndex;
    this.stopIndex = stopIndex;
    this.transitDataZoneId = transitDataZoneId;
  }

  public int getIndexByStop(Stop stop) {
    return stopIndex.indexByStop.get(stop);
  }

  public Stop getStopByIndex(int stop) {
    return stop != -1 ? this.stopIndex.stopsByIndex.get(stop) : null;
  }

  public StopIndexForRaptor getStopIndex() {
    return this.stopIndex;
  }

  public Collection<TripPatternForDate> getTripPatternsForDate(LocalDate date) {
    return tripPatternsForDate.getOrDefault(date, Collections.emptyList());
  }

  /**
   * This is the time zone witch is used for interpreting all local "service" times
   * (in transfers, trip schedules and so on). This is the time zone of the internal OTP
   * time - which is used in logging and debugging. This is independent of the time zone
   * of imported data and of the time zone used on any API - it can be the same, but it does
   * not need to.
   */
  public ZoneId getTransitDataZoneId() {
    return transitDataZoneId;
  }

  public int getStopCount() {
    return stopIndex.stopsByIndex.size();
  }

  public List<TripPatternForDate> getTripPatternsForDateCopy(LocalDate date) {
    List<TripPatternForDate> tripPatternForDate = tripPatternsForDate.get(date);
    return tripPatternForDate != null ? new ArrayList<>(tripPatternsForDate.get(date)) : null;
  }

  public List<List<Transfer>> getTransferByStopIndex() {
    return this.transferByStopIndex;
  }

  /**
   * Replaces all the TripPatternForDates for a single date. This is an atomic operation according
   * to the HashMap implementation.
   */
  public void replaceTripPatternsForDate(
      LocalDate date,
      List<TripPatternForDate> tripPatternForDates
  ) {
    this.tripPatternsForDate.replace(date, tripPatternForDates);
  }
}
