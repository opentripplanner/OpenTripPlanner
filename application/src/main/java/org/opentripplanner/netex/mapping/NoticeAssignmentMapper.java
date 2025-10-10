package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Maps NeTEx NoticeAssignment, which is the connection between a Notice and the object it refers
 * to. In the case of a notice referring to a StopPointInJourneyPattern, which has no OTP
 * equivalent, it will be assigned to its corresponding TimeTabledPassingTimes for each
 * ServiceJourney in the same JourneyPattern.
 */
class NoticeAssignmentMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  private final Multimap<String, TimetabledPassingTime> passingTimeByStopPointId =
    ArrayListMultimap.create();

  private final ReadOnlyHierarchicalMap<String, org.rutebanken.netex.model.Notice> noticesById;

  private final EntityById<Route> routesById;

  private final EntityById<Trip> tripsById;

  private final Map<String, StopTime> stopTimesByNetexId;

  /** Note! The notice mapper caches notices, making sure duplicates are not created. */
  private final NoticeMapper noticeMapper;

  NoticeAssignmentMapper(
    DataImportIssueStore issueStore,
    FeedScopedIdFactory idFactory,
    Collection<ServiceJourney> serviceJourneys,
    ReadOnlyHierarchicalMap<String, org.rutebanken.netex.model.Notice> noticesById,
    EntityById<Route> routesById,
    EntityById<Trip> tripsById,
    Map<String, StopTime> stopTimesByNetexId
  ) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
    this.noticeMapper = new NoticeMapper(idFactory);
    this.noticesById = noticesById;
    this.routesById = routesById;
    this.tripsById = tripsById;
    this.stopTimesByNetexId = stopTimesByNetexId;

    // Index passing time by stopPoint id
    for (ServiceJourney sj : serviceJourneys) {
      for (TimetabledPassingTime it : sj.getPassingTimes().getTimetabledPassingTime()) {
        passingTimeByStopPointId.put(it.getPointInJourneyPatternRef().getValue().getRef(), it);
      }
    }
  }

  Multimap<AbstractTransitEntity, Notice> map(NoticeAssignment noticeAssignment) {
    // TODO OTP2 - Idealy this should en up as one key,value pair.
    //             The `StopPointInJourneyPattern` which result in more than one key/valye pair,
    //             can be replaced with a new compound key type.
    Multimap<AbstractTransitEntity, Notice> noticiesByEntity = ArrayListMultimap.create();

    String noticedObjectId = noticeAssignment.getNoticedObjectRef().getRef();
    Notice otpNotice = getOrMapNotice(noticeAssignment);

    if (otpNotice == null) {
      issueStore.add(
        "NoticeAssignmentWithoutNotice",
        "Notice in notice assignment is missing for assignment %s",
        noticeAssignment
      );
      return noticiesByEntity;
    }

    // Special case for StopPointInJourneyPattern. The OTP model do not have this element, so
    // we attach the notice to all StopTimes for the pattern at the given stop.
    Collection<TimetabledPassingTime> times = passingTimeByStopPointId.get(noticedObjectId);
    if (times != null && !times.isEmpty()) {
      for (TimetabledPassingTime time : times) {
        addStopTimeNotice(noticiesByEntity, time.getId(), otpNotice);
      }
    } else if (stopTimesByNetexId.containsKey(noticedObjectId)) {
      addStopTimeNotice(noticiesByEntity, noticedObjectId, otpNotice);
    } else {
      FeedScopedId otpId = idFactory.createId(noticedObjectId);

      if (routesById.containsKey(otpId)) {
        noticiesByEntity.put(routesById.get(otpId), otpNotice);
      } else if (tripsById.containsKey(otpId)) {
        noticiesByEntity.put(tripsById.get(otpId), otpNotice);
      } else {
        issueStore.add(
          "NoticeAssignmentWithUnknownEntity",
          "Could not map notice assignment %s for element with id %s",
          noticeAssignment.getId(),
          noticedObjectId
        );
      }
    }
    return noticiesByEntity;
  }

  @Nullable
  private Notice getOrMapNotice(NoticeAssignment assignment) {
    org.rutebanken.netex.model.Notice notice = assignment.getNotice() != null
      ? assignment.getNotice()
      : noticesById.lookup(assignment.getNoticeRef().getRef());

    return notice == null ? null : noticeMapper.map(notice);
  }

  private void addStopTimeNotice(
    Multimap<AbstractTransitEntity, Notice> map,
    String stopTimeId,
    Notice notice
  ) {
    StopTime stopTime = stopTimesByNetexId.get(stopTimeId);
    if (stopTime == null) {
      issueStore.add(
        "NoticeAssigmentWithoutStopTime",
        "NoticeAssigment mapping failed, StopTime not found. StopTime id: %s",
        stopTimeId
      );
      return;
    }
    map.put(stopTime.getId(), notice);
  }
}
