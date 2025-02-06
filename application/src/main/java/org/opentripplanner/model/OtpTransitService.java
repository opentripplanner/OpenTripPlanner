package org.opentripplanner.model;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.Pathway;
import org.opentripplanner.transit.model.site.PathwayNode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepository;

/**
 * Methods for accessing imported entities.
 */
public interface OtpTransitService {
  /**
   * @return a list of all Agencies.
   */
  Collection<Agency> getAllAgencies();

  /**
   * @return a list of all Operators, the list may be empty if there are no Operators in the
   * imported data.
   */
  Collection<Operator> getAllOperators();

  Collection<FeedInfo> getAllFeedInfos();

  SiteRepository siteRepository();

  /**
   * This is equivalent to a Transmodel Notice Assignments. The map key may reference entity ids of
   * any type (Serializable).
   */
  Multimap<AbstractTransitEntity, Notice> getNoticeAssignments();

  Collection<Pathway> getAllPathways();

  /**
   * @return all ids for both Calendars and CalendarDates merged into on list without duplicates.
   */
  Collection<FeedScopedId> getAllServiceIds();

  List<ShapePoint> getShapePointsForShapeId(FeedScopedId shapeId);

  Collection<Entrance> getAllEntrances();

  Collection<PathwayNode> getAllPathwayNodes();

  Collection<BoardingArea> getAllBoardingAreas();

  /**
   * @return the list of {@link StopTime} objects associated with the trip, sorted by {@link
   * StopTime#getStopSequence()}
   */
  List<StopTime> getStopTimesForTrip(Trip trip);

  Collection<ConstrainedTransfer> getAllTransfers();

  Collection<TripPattern> getTripPatterns();

  Collection<Trip> getAllTrips();

  Collection<FlexTrip<?, ?>> getAllFlexTrips();

  /**
   * @return if transit service has any active services. The graph build might filter out all
   * transit services if they are outside the configured 'transitServiceStart' and 'transitServiceEnd'
   */
  boolean hasActiveTransit();

  /**
   * @see org.opentripplanner.transit.service.TimetableRepository#findStopByScheduledStopPoint(FeedScopedId)
   */
  Map<FeedScopedId, RegularStop> stopsByScheduledStopPoint();
}
