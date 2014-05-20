/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.request.BannedStopSet;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatternInterlineDwellTest extends GtfsTest {

    @Override
    public boolean isLongDistance() { return true; } // retrying wrecks the tests

    @Override
    public String getFeedName() {
        return "gtfs/interlining";
    }

    // TODO Allow using Calendar or ISOdate for testing, interpret it in the given graph's timezone.

    public void testInterlining() {
        Calendar calendar = new GregorianCalendar(2014, Calendar.JANUARY, 01, 00, 05, 00);
        calendar.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        long time = calendar.getTime().getTime() / 1000;
        // We should arrive at the destination using two legs, both of which are on
        // the same route and with zero transfers.
        Leg[] legs = plan(time, "stop0", "stop3", null, false, false, null, null, null, 2);
        assertEquals(legs[0].routeId, "route1");
        assertEquals(legs[1].routeId, "route1");
        assertTrue(itinerary.transfers == 0);
    }

    // TODO test for trips on the same block with no transfer allowed (Trimet special case)

}
