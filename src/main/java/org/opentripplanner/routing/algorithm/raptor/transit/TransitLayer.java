package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.Stop;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TransitLayer {

  /**
   * Transit data required for routing
   */
  private final Map<LocalDate, List<TripPatternForDate>> tripPatternsForDate;

  /**
   * Index of outer list is from stop index, inner list index has no specific meaning. To stop index
   * is a field of the Transfer object.
   */
  private final List<List<Transfer>> transferByStopIndex;

  /**
   * Maps to original graph to retrieve additional data
   */
  private final List<Stop> stopsByIndex;


  private final Map<Stop, Integer> indexByStop;

  private final ZoneId transitDataZoneId;


  public TransitLayer(
      Map<LocalDate,
      List<TripPatternForDate>> tripPatternsForDate,
      List<List<Transfer>> transferByStopIndex,
      List<Stop> stopsByIndex,
      Map<Stop,
      Integer> indexByStop,
      ZoneId transitDataZoneId
  ) {
    this.tripPatternsForDate = tripPatternsForDate;
    this.transferByStopIndex = transferByStopIndex;
    this.stopsByIndex = stopsByIndex;
    this.indexByStop = indexByStop;
    this.transitDataZoneId = transitDataZoneId;
  }

  public int getIndexByStop(Stop stop) {
    return indexByStop.get(stop);
  }

  public Stop getStopByIndex(int stopIndex) {
    return stopIndex != -1 ? stopsByIndex.get(stopIndex) : null;
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
    return stopsByIndex.size();
  }

  public List<List<Transfer>> getTransferByStopIndex() {
    return this.transferByStopIndex;
  }
}
