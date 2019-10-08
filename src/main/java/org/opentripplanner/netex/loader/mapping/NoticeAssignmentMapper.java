package org.opentripplanner.netex.loader.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.StopTimeKey;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalMap;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

import static org.opentripplanner.netex.loader.mapping.FeedScopedIdFactory.createFeedScopedId;

/**
 * Maps NeTEx NoticeAssignment, which is the connection between a Notice and the object it refers
 * to. In the case of a notice referring to a StopPointInJourneyPattern, which has no OTP equivalent,
 * it will be assigned to its corresponding TimeTabledPassingTimes for each ServiceJourney in the
 * same JourneyPattern.
 */
class NoticeAssignmentMapper {

    private final ReadOnlyHierarchicalMap<String, Collection<TimetabledPassingTime>> passingTimeByStopPointId;

    private final ReadOnlyHierarchicalMap<String, org.rutebanken.netex.model.Notice> noticesById;

    private final EntityById<FeedScopedId, Route> routesById;

    private final EntityById<FeedScopedId, Trip> tripsById;

    private final Map<String, StopTime> stopTimesByNetexId;

    /** Note! The notce mapper cashes notices, making sure duplicates are not created. */
    private final NoticeMapper noticeMapper = new NoticeMapper();

    private static final Logger LOG = LoggerFactory.getLogger(NoticeAssignmentMapper.class);

    NoticeAssignmentMapper(
            ReadOnlyHierarchicalMap<String, Collection<TimetabledPassingTime>> passingTimeByStopPointId,
            ReadOnlyHierarchicalMap<String, org.rutebanken.netex.model.Notice> noticesById,
            EntityById<FeedScopedId, Route> routesById,
            EntityById<FeedScopedId, Trip> tripsById,
            Map<String, StopTime> stopTimesByNetexId
    ) {
        this.passingTimeByStopPointId = passingTimeByStopPointId;
        this.noticesById = noticesById;
        this.routesById = routesById;
        this.tripsById = tripsById;
        this.stopTimesByNetexId = stopTimesByNetexId;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    Multimap<TransitEntity<?>, Notice> map(NoticeAssignment noticeAssignment){
        Multimap<TransitEntity<?>, Notice> noticeByElement = ArrayListMultimap.create();

        String noticedObjectId = noticeAssignment.getNoticedObjectRef().getRef();
        Notice otpNotice = getOrMapNotice(noticeAssignment);

        if(otpNotice == null) {
            LOG.warn("Notice in notice assignment is missing for assignment {}", noticeAssignment);
            return noticeByElement;
        }

        // Special case for StopPointInJourneyPattern. The OTP model do not have this element, so we
        // attach the notice to all StopTimes for the pattern at the given stop.
        Collection<TimetabledPassingTime> times =  passingTimeByStopPointId.lookup(noticedObjectId);
        if (!times.isEmpty()) {
            for (TimetabledPassingTime time : times) {
                noticeByElement.put(lookupStopTimeKey(time.getId()), otpNotice);
            }
        } else if (stopTimesByNetexId.containsKey(noticedObjectId)) {
            noticeByElement.put(lookupStopTimeKey(noticedObjectId), otpNotice);
        } else {
            FeedScopedId otpId = createFeedScopedId(noticedObjectId);

            if(addNotice(routesById.get(otpId), otpNotice, noticeByElement)) { /* intentionally empty */ }
            else if(addNotice(tripsById.get(otpId), otpNotice, noticeByElement)) { /* intentionally empty */ }
            else {
                LOG.warn("Could not map noticeAssignment for element with id {}", noticedObjectId);
            }
        }
        return noticeByElement;
    }

    private StopTimeKey lookupStopTimeKey(String timeTablePassingTimeId) {
        return stopTimesByNetexId.get(timeTablePassingTimeId).getId();
    }

    private Notice getOrMapNotice(NoticeAssignment assignment) {
        org.rutebanken.netex.model.Notice notice = assignment.getNotice() != null
                ? assignment.getNotice()
                : noticesById.lookup(assignment.getNoticeRef().getRef());

        return notice == null ? null : noticeMapper.map(notice);
    }

    private static boolean addNotice(
            TransitEntity entity,
            Notice otpNotice,
            Multimap<TransitEntity<?>, Notice> noticeByEntity
    ) {
        if(entity == null) return false;
        noticeByEntity.put(entity, otpNotice);
        return true;
    }
}