package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.RouterConfig.DEFAULT;

import java.time.OffsetDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;

class RouterConfigTest {

    @Test
    void middleOfDay() {
        var req = getReq("2022-01-25T13:14:20+01:00");
        assertEquals(0, DEFAULT.additionalSearchDaysInFuture(req));
        assertEquals(0, DEFAULT.additionalSearchDaysInPast(req));

        req.arriveBy = true;
        assertEquals(0, DEFAULT.additionalSearchDaysInFuture(req));
        assertEquals(0, DEFAULT.additionalSearchDaysInPast(req));
    }

    @Test
    void closeToMidnight() {
        var req = getReq("2022-01-25T23:14:20+01:00");

        assertEquals(0, DEFAULT.additionalSearchDaysInPast(req));
        assertEquals(1, DEFAULT.additionalSearchDaysInFuture(req));

        req.arriveBy = true;
        assertEquals(0, DEFAULT.additionalSearchDaysInPast(req));
        assertEquals(0, DEFAULT.additionalSearchDaysInFuture(req));
    }

    @Test
    void shortlyAfterMidnight() {
        var req = getReq("2022-01-25T00:15:25+01:00");

        assertEquals(0, DEFAULT.additionalSearchDaysInPast(req));
        assertEquals(0, DEFAULT.additionalSearchDaysInFuture(req));

        req.arriveBy = true;
        assertEquals(1, DEFAULT.additionalSearchDaysInPast(req));
        assertEquals(0, DEFAULT.additionalSearchDaysInFuture(req));
    }

    private RoutingRequest getReq(String time) {
        var feedId = new FeedScopedId("1", "1");

        var cal = new CalendarServiceData();
        cal.putTimeZoneForAgencyId(feedId, TimeZone.getTimeZone("Europe/Berlin"));

        var graph = new Graph();
        graph.putService(CalendarServiceData.class, cal);
        graph.addAgency("test", new Agency(feedId, "test", "Europe/Berlin"));

        var req = new RoutingRequest();
        req.setRoutingContext(graph);
        req.setDateTime(OffsetDateTime.parse(time).toInstant());

        return req;
    }

}