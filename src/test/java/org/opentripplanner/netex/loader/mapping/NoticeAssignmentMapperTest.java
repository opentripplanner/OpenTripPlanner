package org.opentripplanner.netex.loader.mapping;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
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
    private static final String TIMETABLED_PASSING_TIME2 = "RUT:TimetabledPassingTime:2";


    private static final Notice NOTICE = new org.rutebanken.netex.model.Notice()
            .withId(NOTICE_ID)
            .withPublicCode("Notice Code")
            .withText(new MultilingualString().withValue("Notice text"));

    @Test
    public void mapNoticeAssignment() {
        NoticeAssignment noticeAssignment = new NoticeAssignment();

        noticeAssignment.setNoticedObjectRef(new VersionOfObjectRefStructure().withRef(ROUTE_ID));
        noticeAssignment.setNotice(NOTICE);

        Route route = new Route(MappingSupport.ID_FACTORY.createId(ROUTE_ID));

        EntityById<Route> routesById = new EntityById<>();
        routesById.add(route);

        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                MappingSupport.ID_FACTORY,
                new HierarchicalMultimap<>(),
                new HierarchicalMapById<>(),
                routesById,
                new EntityById<>(),
                new HashMap<>()
        );

        Multimap<TransitEntity, org.opentripplanner.model.Notice> noticesByElement =
                noticeAssignmentMapper.map(noticeAssignment);

        org.opentripplanner.model.Notice notice2 = noticesByElement.get(route).iterator().next();

        assertEquals(NOTICE_ID, notice2.getId().getId());
    }

    @Test
    public void mapNoticeAssignmentOnStopPoint() {
        HierarchicalMultimap<String, TimetabledPassingTime> passingTimeByStopPointId =
                new HierarchicalMultimap<>();
        HierarchicalMapById<Notice> noticesById = new HierarchicalMapById<>();


        passingTimeByStopPointId.add(
                STOP_POINT_ID,
                new TimetabledPassingTime().withId(TIMETABLED_PASSING_TIME1)
        );
        passingTimeByStopPointId.add(
                STOP_POINT_ID,
                new TimetabledPassingTime().withId(TIMETABLED_PASSING_TIME2)
        );

        Trip trip = new Trip(new FeedScopedId("T", "1"));
        StopTime stopTime1 = createStopTime(1, trip);
        StopTime stopTime2 = createStopTime(2, trip);

        Map<String, StopTime> stopTimesById = new HashMap<>();
        stopTimesById.put(TIMETABLED_PASSING_TIME1, stopTime1);
        stopTimesById.put(TIMETABLED_PASSING_TIME2, stopTime2);

        noticesById.add(NOTICE);

        NoticeAssignment noticeAssignment = new NoticeAssignment()
                .withNoticedObjectRef(new VersionOfObjectRefStructure().withRef(STOP_POINT_ID))
                .withNoticeRef(new NoticeRefStructure().withRef(NOTICE_ID));

        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                MappingSupport.ID_FACTORY,
                passingTimeByStopPointId,
                noticesById,
                new EntityById<>(),
                new EntityById<>(),
                stopTimesById
        );

        Multimap<TransitEntity, org.opentripplanner.model.Notice> noticesByElement
                = noticeAssignmentMapper.map(noticeAssignment);

        org.opentripplanner.model.Notice notice2a = noticesByElement.get(stopTime1.getId())
                .stream().findFirst().orElseThrow(IllegalStateException::new);

        org.opentripplanner.model.Notice notice2b = noticesByElement.get(stopTime2.getId())
                .stream().findFirst().orElseThrow(IllegalStateException::new);

        assertEquals(NOTICE_ID, notice2a.getId().getId());
        assertEquals(NOTICE_ID, notice2b.getId().getId());
    }

    private StopTime createStopTime(int stopSequence, Trip trip) {
        StopTime stopTime1 = new StopTime();
        stopTime1.setStopSequence(stopSequence);
        stopTime1.setTrip(trip);
        return stopTime1;
    }
}
