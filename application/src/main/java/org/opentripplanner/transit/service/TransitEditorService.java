package org.opentripplanner.transit.service;

import java.time.LocalDate;
import javax.annotation.Nullable;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * Entry point for requests (both read-only and read-write) towards the transit API.
 */
public interface TransitEditorService extends TransitService {
  void addAgency(Agency agency);

  void addFeedInfo(FeedInfo info);

  void addRoutes(Route route);

  void addTransitMode(TransitMode mode);

  FeedScopedId getOrCreateServiceIdForDate(LocalDate serviceDate);

  /**
   * Return the trip for the given id, not including trips created in real time.
   */
  @Nullable
  Trip getScheduledTrip(FeedScopedId id);

  /**
   * Set the original, immutable, transit layer,
   * based on scheduled data (not real-time data).
   */
  void setTransitLayer(TransitLayer transitLayer);

  /**
   * Return true if a real-time transit layer is present.
   * The real-time transit layer is optional,
   * it is present only when real-time updaters are configured.
   */
  boolean hasRealtimeTransitLayer();

  /**
   * Publish the latest snapshot of the real-time transit layer.
   * Should be called only when creating a new TransitLayer, from the graph writer thread.
   */
  void setRealtimeTransitLayer(TransitLayer realtimeTransitLayer);
}
