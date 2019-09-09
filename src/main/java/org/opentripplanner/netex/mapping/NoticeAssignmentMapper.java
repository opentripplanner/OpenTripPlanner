package org.opentripplanner.netex.mapping;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.*;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

/**
 * Maps NeTEx NoticeAssignment, which is the connection between a Notice and the object it refers
 * to. In the case of a notice referring to a StopPointInJourneyPattern, which has no OTP equivalent,
 * it will be assigned to its corresponding TimeTabledPassingTimes for each ServiceJourney in the
 * same JourneyPattern.
 *
 * In order to maintain this connection to TimeTabledPassingTime (StopTime in OTP), it is necessary
 * to assign the TimeTabledPassingTime id to its corresponding StopTime.
 */
class NoticeAssignmentMapper {

    private final HierarchicalMultimap<String, TimetabledPassingTime> passingTimeByStopPointId;

    private final HierarchicalMapById<org.rutebanken.netex.model.Notice> noticesById;

    private final EntityById<FeedScopedId, Route> routesById;

    private final Map<String, StopTime> stopTimesById;

    private final EntityById<FeedScopedId, Trip> tripsById;

    /** Note! The notce mapper cashes notices, making sure duplicates are not created. */
    private final NoticeMapper noticeMapper = new NoticeMapper();

    private static final Logger LOG = LoggerFactory.getLogger(NoticeAssignmentMapper.class);

    NoticeAssignmentMapper(
            HierarchicalMultimap<String, TimetabledPassingTime> passingTimeByStopPointId,
            HierarchicalMapById<org.rutebanken.netex.model.Notice> noticesById,
            EntityById<FeedScopedId, Route> routesById,
            Map<String, StopTime> stopTimesById,
            EntityById<FeedScopedId, Trip> tripsById
    ) {
        this.passingTimeByStopPointId = passingTimeByStopPointId;
        this.noticesById = noticesById;
        this.routesById = routesById;
        this.stopTimesById = stopTimesById;
        this.tripsById = tripsById;
    }

    Multimap<NoticeAssignable, Notice> map(NoticeAssignment noticeAssignment){
        Multimap<NoticeAssignable, Notice> noticeByElement = HashMultimap.create();

        String noticedObjectId = noticeAssignment.getNoticedObjectRef().getRef();
        Notice otpNotice = getOrMapNotice(noticeAssignment);

        if(otpNotice == null) {
            LOG.warn("Notice in notice assignment is missing for assignment {}", noticeAssignment);
            return noticeByElement;
        }

        Collection<TimetabledPassingTime> times =  passingTimeByStopPointId.lookup(noticedObjectId);

        // Special case for StopPointInJourneyPattern
        if (!times.isEmpty()) {
            for (TimetabledPassingTime time : times) {
                noticeByElement.put(
                        stopTimesById.get(time.getId()),
                        otpNotice
                );
            }
        } else if (stopTimesById.containsKey(noticedObjectId)) {
            noticeByElement.put(stopTimesById.get(noticedObjectId), otpNotice);
        } else if (routesById.containsKey(createFeedScopedId(noticedObjectId))) {
            noticeByElement.put(routesById.get(createFeedScopedId(noticedObjectId)), otpNotice);
        } else if (tripsById.containsKey(createFeedScopedId(noticedObjectId))) {
            noticeByElement.put(tripsById.get(createFeedScopedId(noticedObjectId)), otpNotice);
        } else {
            LOG.warn("Could not map noticeAssignment for element with id {}", noticedObjectId);
        }

        return noticeByElement;
    }

    private Notice getOrMapNotice(NoticeAssignment assignment) {
        org.rutebanken.netex.model.Notice notice = assignment.getNotice() != null
                ? assignment.getNotice()
                : noticesById.lookup(assignment.getNoticeRef().getRef());

        return notice == null ? null : noticeMapper.map(notice);
    }
}