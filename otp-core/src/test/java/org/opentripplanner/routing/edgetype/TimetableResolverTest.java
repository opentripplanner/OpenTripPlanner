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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.common.IterableLibrary.filter;

import java.io.File;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

public class TimetableResolverTest {
    private static Graph graph;
    private static GtfsContext context;
    private static Map<AgencyAndId, TableTripPattern> patternIndex;
    
    @BeforeClass
    public static void setUp() throws Exception {

        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));
        graph = new Graph();

        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);
        graph.putService(CalendarServiceData.class, GtfsLibrary.createCalendarServiceData(context.getDao()));
        
        patternIndex = new HashMap<AgencyAndId, TableTripPattern>();
        for (TransitStopDepart tsd : filter(graph.getVertices(), TransitStopDepart.class)) {
            for (TransitBoardAlight tba : filter(tsd.getOutgoing(), TransitBoardAlight.class)) {
                if (!tba.isBoarding())
                    continue;
                TableTripPattern pattern = tba.getPattern();
                for (Trip trip : pattern.getTrips()) {
                    patternIndex.put(trip.getId(), pattern);
                }
            }
        }
    }
    
    @Test
    public void testCompare() {
        Timetable orig = new Timetable(null);
        Timetable a = orig.copy(new ServiceDate().previous());
        Timetable b = orig.copy(new ServiceDate());
        assertEquals(-1, (new TimetableResolver.SortedTimetableComparator()).compare(a, b));
    }
    
    @Test
    public void testResolve() {
        ServiceDate today = new ServiceDate();
        ServiceDate yesterday = today.previous();
        ServiceDate tomorrow = today.next();
        TableTripPattern pattern = patternIndex.get(new AgencyAndId("agency", "1.1"));
        TimetableResolver resolver = new TimetableResolver();
        
        Timetable scheduled = resolver.resolve(pattern, today);
        assertEquals(scheduled, resolver.resolve(pattern, null));
        resolver.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, today));
        
        // add a new timetable for today
        Timetable forNow = resolver.resolve(pattern, today);
        assertEquals(scheduled, resolver.resolve(pattern, yesterday));
        assertNotSame(scheduled, forNow);
        assertEquals(scheduled, resolver.resolve(pattern, tomorrow));
        assertEquals(scheduled, resolver.resolve(pattern, null));
        
        // add a new timetable for yesterday
        resolver.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, yesterday));
        Timetable forYesterday = resolver.resolve(pattern, yesterday);
        assertNotSame(scheduled, forYesterday);
        assertNotSame(scheduled, forNow);
        assertEquals(scheduled, resolver.resolve(pattern, tomorrow));
        assertEquals(scheduled, resolver.resolve(pattern, null));
    }
    
    @Test(expected=ConcurrentModificationException.class)
    public void testUpdate() {
        ServiceDate today = new ServiceDate();
        ServiceDate yesterday = today.previous();
        TableTripPattern pattern = patternIndex.get(new AgencyAndId("agency", "1.1"));
        
        TimetableResolver resolver = new TimetableResolver();
        Timetable origNow = resolver.resolve(pattern, today);
        
        // new timetable for today
        resolver.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, today));
        Timetable updatedNow = resolver.resolve(pattern, today);
        assertNotSame(origNow, updatedNow);

        // reuse timetable for today
        resolver.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, today));
        assertEquals(updatedNow, resolver.resolve(pattern, today));
        
        // create new timetable for tomorrow
        resolver.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, yesterday));
        assertNotSame(origNow, resolver.resolve(pattern, yesterday));
        assertNotSame(updatedNow, resolver.resolve(pattern, yesterday));
        
        // exception if we try to modify a snapshot
        TimetableResolver snapshot = resolver.commit();
        snapshot.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, yesterday));
    }
    
    public void testCommit() {
        ServiceDate today = new ServiceDate();
        ServiceDate yesterday = today.previous();
        TableTripPattern pattern = patternIndex.get(new AgencyAndId("agency", "1.1"));

        TimetableResolver resolver = new TimetableResolver();
        
        // only return a new snapshot if there are changes
        TimetableResolver snapshot = resolver.commit();
        assertNull(snapshot);
                
        // add a new timetable for today, commit, and everything should match
        assertTrue(resolver.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, today)));
        snapshot = resolver.commit();
        assertEquals(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));
        
        // add a new timetable for today, don't commit, and everything should not match
        assertTrue(resolver.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, today)));
        assertNotSame(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));
        
        // add a new timetable for today, on another day, and things should still not match
        assertTrue(resolver.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, yesterday)));
        assertNotSame(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));

        // commit, and things should match
        snapshot = resolver.commit();
        assertEquals(snapshot.resolve(pattern, today), resolver.resolve(pattern, today));
        assertEquals(snapshot.resolve(pattern, yesterday), resolver.resolve(pattern, yesterday));
        
        assertNull(snapshot.commit());
    }

    @Test
    public void testPurge() {
        ServiceDate today = new ServiceDate();
        ServiceDate yesterday = today.previous();
        TableTripPattern pattern = patternIndex.get(new AgencyAndId("agency", "1.1"));

        TimetableResolver resolver = new TimetableResolver();
        assertTrue(resolver.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, today)));
        assertTrue(resolver.update(pattern, TripUpdateList.forCanceledTrip(pattern.getTrip(0).getId(), 0, yesterday)));
        
        assertNotSame(resolver.resolve(pattern, yesterday), resolver.resolve(pattern, null));
        assertNotSame(resolver.resolve(pattern, today), resolver.resolve(pattern, null));
        
        assertNotNull(resolver.commit());
        assertFalse(resolver.isDirty());
        
        assertTrue(resolver.purgeExpiredData(yesterday));
        assertFalse(resolver.purgeExpiredData(yesterday));
        
        assertEquals(resolver.resolve(pattern, yesterday), resolver.resolve(pattern, null));
        assertNotSame(resolver.resolve(pattern, today), resolver.resolve(pattern, null));
        
        assertNull(resolver.commit());
        assertFalse(resolver.isDirty());
    }
}
