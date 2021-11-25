package org.opentripplanner.netex.mapping;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.NoticeRefStructure;
import org.rutebanken.netex.model.PointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

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
                new DataImportIssueStore(false),
                MappingSupport.ID_FACTORY,
                List.of(),
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
        HierarchicalMapById<Notice> noticesById = new HierarchicalMapById<>();

        Collection<ServiceJourney> serviceJourneys = List.of(
            new ServiceJourney().withPassingTimes(
                new TimetabledPassingTimes_RelStructure().withTimetabledPassingTime(
                    createTimetabledPassingTime(TIMETABLED_PASSING_TIME1, STOP_POINT_ID),
                    createTimetabledPassingTime(TIMETABLED_PASSING_TIME2, STOP_POINT_ID)
                )
            )
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
                new DataImportIssueStore(false),
                MappingSupport.ID_FACTORY,
                serviceJourneys,
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

    private static TimetabledPassingTime createTimetabledPassingTime(String id, String stopPointId) {
        return new TimetabledPassingTime()
            .withId(id)
            .withPointInJourneyPatternRef(
                MappingSupport.createJaxbElement(
                    new PointInJourneyPatternRefStructure().withRef(stopPointId)
                )
            );
    }
}
