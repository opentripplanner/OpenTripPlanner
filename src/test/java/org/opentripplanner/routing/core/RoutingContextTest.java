package org.opentripplanner.routing.core;

import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;

import java.util.Collections;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoutingContextTest {

    @Test
    public void testSetServiceDays() throws Exception {

        String feedId = "FEED";
        String agencyId = "AGENCY";

        Agency agency = new Agency();
        agency.setId(agencyId);

        Graph graph = mock(Graph.class);
        RoutingRequest routingRequest = mock(RoutingRequest.class);
        CalendarService calendarService = mock(CalendarService.class);

        // You're probably not supposed to do this to mocks (access their fields directly)
        // But I know of no other way to do this since the mock object has only action-free stub methods.
        routingRequest.modes = new TraverseModeSet("WALK,TRANSIT");

        when(graph.getTimeZone()).thenReturn(TimeZone.getTimeZone("Europe/Budapest"));
        when(graph.getAllTimeZones()).thenReturn(Collections.singletonList(
                TimeZone.getTimeZone("Europe/Budapest")));
        when(graph.getCalendarService()).thenReturn(calendarService);
        when(graph.getFeedIds()).thenReturn(Collections.singletonList("FEED"));
        when(graph.getAgencies(feedId)).thenReturn(Collections.singletonList(agency));
        when(calendarService.getTimeZoneForAgencyId(agencyId)).thenReturn(TimeZone.getTimeZone("Europe/Budapest"));

        when(routingRequest.getSecondsSinceEpoch())
            .thenReturn(
                1393750800L /* 2014-03-02T10:00:00+01:00 */,

                1396126800L /* 2014-03-29T22:00:00+01:00 */,
                1396132200L /* 2014-03-29T23:30:00+01:00 */,
                1396135800L /* 2014-03-30T00:30:00+01:00 */,

                1401696000L /* 2014-06-02T10:00:00+02:00 */,

                1414272600L /* 2014-10-25T23:30:00+02:00 */,
                1414276200L /* 2014-10-26T00:30:00+02:00 */,
                1414279800L /* 2014-10-26T01:30:00+02:00 */
            );

        verifyServiceDays(routingRequest, graph, new ServiceDate(2014,  3,  1), new ServiceDate(2014,  3,  2), new ServiceDate(2014,  3,  3));

        verifyServiceDays(routingRequest, graph, new ServiceDate(2014,  3, 28), new ServiceDate(2014,  3, 29), new ServiceDate(2014,  3, 30));
        verifyServiceDays(routingRequest, graph, new ServiceDate(2014,  3, 28), new ServiceDate(2014,  3, 29), new ServiceDate(2014,  3, 30));
        verifyServiceDays(routingRequest, graph, new ServiceDate(2014,  3, 29), new ServiceDate(2014,  3, 30), new ServiceDate(2014,  3, 31));

        verifyServiceDays(routingRequest, graph, new ServiceDate(2014,  6,  1), new ServiceDate(2014,  6,  2), new ServiceDate(2014,  6,  3));

        verifyServiceDays(routingRequest, graph, new ServiceDate(2014, 10, 24), new ServiceDate(2014, 10, 25), new ServiceDate(2014, 10, 26));
        verifyServiceDays(routingRequest, graph, new ServiceDate(2014, 10, 25), new ServiceDate(2014, 10, 26), new ServiceDate(2014, 10, 27));
        verifyServiceDays(routingRequest, graph, new ServiceDate(2014, 10, 25), new ServiceDate(2014, 10, 26), new ServiceDate(2014, 10, 27));
    }

    private void verifyServiceDays(RoutingRequest routingRequest, Graph graph, ServiceDate ... dates) {
        RoutingContext routingContext = new RoutingContext(routingRequest, graph, null, mock(PatternArriveVertex.class));

        for(int i = 0; i < dates.length; ++i) {
            assertEquals("date " + i, dates[i], routingContext.serviceDays.get(i).getServiceDate());
        }
    }
}
