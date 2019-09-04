package org.opentripplanner.netex.mapping;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.NoticeAssignable;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.NoticeRefStructure;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class NoticeAssignmentMapperTest {

    private static final String ROUTE_ID = "RUT:Route:1";
    private static final String STOP_POINT_ID = "RUT:StopPointInJourneyPattern:1";
    private static final String NOTICE_ID = "RUT:Notice:1";
    private static final String TIMETABLED_PASSING_TIME1 = "RUT:TimetabledPassingTime:1";
    private static final String TIMETABLED_PASSING_TIME2 = "RUT:TimetabledPassingTime:1";


    private static final Notice NOTICE = new org.rutebanken.netex.model.Notice()
            .withId(NOTICE_ID)
            .withPublicCode("Notice Code")
            .withText(new MultilingualString().withValue("Notice text"));

    @Test
    public void mapNoticeAssignment() {
        NoticeAssignment noticeAssignment = new NoticeAssignment();

        noticeAssignment.setNoticedObjectRef(new VersionOfObjectRefStructure().withRef(ROUTE_ID));
        noticeAssignment.setNotice(NOTICE);

        Route route = new Route();
        route.setId(FeedScopedIdFactory.createFeedScopedId(ROUTE_ID));

        EntityById<FeedScopedId, Route> routesById = new EntityById<>();
        routesById.add(route);

        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                new HierarchicalMultimap<>(),
                new HierarchicalMapById<>(),
                routesById,
                new HashMap<>(),
                new EntityById<>()
        );

        Multimap<NoticeAssignable, org.opentripplanner.model.Notice> noticesByElement = noticeAssignmentMapper.map(noticeAssignment);

        org.opentripplanner.model.Notice notice2 = noticesByElement.get(route)
                .stream().findFirst().get();

        assertEquals(NOTICE_ID, notice2.getId().getId());
    }

    @Test
    public void mapNoticeAssignmentOnStopPoint() {
        HierarchicalMultimap<String, TimetabledPassingTime> passingTimeByStopPointId = new HierarchicalMultimap<>();
        HierarchicalMapById<Notice> noticesById = new HierarchicalMapById<>();


        passingTimeByStopPointId.add(
                STOP_POINT_ID,
                new TimetabledPassingTime().withId(TIMETABLED_PASSING_TIME1)
        );
        passingTimeByStopPointId.add(
                STOP_POINT_ID,
                new TimetabledPassingTime().withId(TIMETABLED_PASSING_TIME2)
        );

        StopTime stopTime1 = new StopTime();
        StopTime stopTime2 = new StopTime();
        stopTime1.setId(FeedScopedIdFactory.createFeedScopedId(TIMETABLED_PASSING_TIME1));
        stopTime2.setId(FeedScopedIdFactory.createFeedScopedId(TIMETABLED_PASSING_TIME2));

        Map<String, StopTime> stopTimesById = new HashMap<>();
        stopTimesById.put(stopTime1.getId().getId(), stopTime1);
        stopTimesById.put(stopTime2.getId().getId(), stopTime2);

        noticesById.add(NOTICE);

        NoticeAssignment noticeAssignment = new NoticeAssignment()
                .withNoticedObjectRef(new VersionOfObjectRefStructure().withRef(STOP_POINT_ID))
                .withNoticeRef(new NoticeRefStructure().withRef(NOTICE_ID));

        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                passingTimeByStopPointId,
                noticesById,
                new EntityById<>(),
                stopTimesById,
                new EntityById<>()
        );

        Multimap<NoticeAssignable, org.opentripplanner.model.Notice> noticesByElementId
                = noticeAssignmentMapper.map(noticeAssignment);

        org.opentripplanner.model.Notice notice2a = noticesByElementId.get(stopTime1)
                .stream().findFirst().orElseThrow(IllegalStateException::new);

        org.opentripplanner.model.Notice notice2b = noticesByElementId.get(stopTime2)
                .stream().findFirst().orElseThrow(IllegalStateException::new);

        assertEquals(NOTICE_ID, notice2a.getId().getId());
        assertEquals(NOTICE_ID, notice2b.getId().getId());
    }
}
