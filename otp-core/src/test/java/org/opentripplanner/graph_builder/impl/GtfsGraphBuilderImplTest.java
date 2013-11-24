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

package org.opentripplanner.graph_builder.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.MockGtfs;
import org.opentripplanner.graph_builder.impl.transit_index.TransitIndexBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.model.GtfsBundles;
import org.opentripplanner.graph_builder.services.GraphBuilderWithGtfsDao;
import org.opentripplanner.gtfs.BikeAccess;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitIndexService;

public class GtfsGraphBuilderImplTest {

    private static final HashMap<Class<?>, Object> _extra = new HashMap<Class<?>, Object>();

    private GtfsGraphBuilderImpl _builder = new GtfsGraphBuilderImpl();

    @Before
    public void setup() {
        GraphBuilderWithGtfsDao indexBuilder = new TransitIndexBuilder();
        _builder.setGtfsGraphBuilders(Arrays.asList(indexBuilder));
    }

    @Test
    public void testNoBikesByDefault() throws IOException, JSONException {
        // We configure two trip: one with unknown bikes_allowed and the second with bikes
        // allowed.
        MockGtfs gtfs = getSimpleGtfs();
        gtfs.putTrips(2, "r0", "sid0", "bikes_allowed=0,1");
        gtfs.putStopTimes("t0,t1", "s0,s1");

        GtfsBundles bundles = getGtfsAsBundles(gtfs);
        bundles.getBundles().get(0).setDefaultBikesAllowed(false);
        _builder.setGtfsBundles(bundles);

        Graph graph = new Graph();
        _builder.buildGraph(graph, _extra);

        TransitIndexService index = graph.getService(TransitIndexService.class);
        TableTripPattern pattern = index.getTripPatternForTrip(new AgencyAndId("a0", "t0"));
        List<Trip> trips = pattern.getTrips();
        assertEquals(BikeAccess.UNKNOWN,
                BikeAccess.fromTrip(withId(trips, new AgencyAndId("a0", "t0"))));
        assertEquals(BikeAccess.ALLOWED,
                BikeAccess.fromTrip(withId(trips, new AgencyAndId("a0", "t1"))));
    }

    @Test
    public void testBikesByDefault() throws IOException, JSONException {
        // We configure two trip: one with unknown bikes_allowed and the second with no bikes
        // allowed.
        MockGtfs gtfs = getSimpleGtfs();
        gtfs.putTrips(2, "r0", "sid0", "bikes_allowed=0,2");
        gtfs.putStopTimes("t0,t1", "s0,s1");

        GtfsBundles bundles = getGtfsAsBundles(gtfs);
        bundles.getBundles().get(0).setDefaultBikesAllowed(true);
        _builder.setGtfsBundles(bundles);

        Graph graph = new Graph();
        _builder.buildGraph(graph, _extra);

        TransitIndexService index = graph.getService(TransitIndexService.class);
        TableTripPattern pattern = index.getTripPatternForTrip(new AgencyAndId("a0", "t0"));
        List<Trip> trips = pattern.getTrips();
        assertEquals(BikeAccess.ALLOWED,
                BikeAccess.fromTrip(withId(trips, new AgencyAndId("a0", "t0"))));
        assertEquals(BikeAccess.NOT_ALLOWED,
                BikeAccess.fromTrip(withId(trips, new AgencyAndId("a0", "t1"))));
    }

    private MockGtfs getSimpleGtfs() throws IOException {
        MockGtfs gtfs = MockGtfs.create();
        gtfs.putAgencies(1);
        gtfs.putRoutes(1);
        gtfs.putStops(2);
        gtfs.putCalendars(1);
        gtfs.putTrips(1, "r0", "sid0");
        gtfs.putStopTimes("t0", "s0,s1");
        return gtfs;
    }

    private static GtfsBundles getGtfsAsBundles(MockGtfs gtfs) {
        GtfsBundle bundle = new GtfsBundle();
        bundle.setPath(gtfs.getPath());
        GtfsBundles bundles = new GtfsBundles();
        bundles.getBundles().add(bundle);
        return bundles;
    }

    private static <S extends Serializable, T extends IdentityBean<S>> T withId(Iterable<T> beans,
            S id) {
        for (T bean : beans) {
            if (bean.getId().equals(id)) {
                return bean;
            }
        }
        return null;
    }
}
