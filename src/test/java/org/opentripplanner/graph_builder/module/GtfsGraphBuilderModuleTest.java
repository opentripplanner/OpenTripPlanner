package org.opentripplanner.graph_builder.module;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import com.beust.jcommander.internal.Lists;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.IdentityBean;
import org.opentripplanner.model.Trip;
import org.opentripplanner.gtfs.MockGtfs;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.gtfs.BikeAccess;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;

public class GtfsGraphBuilderModuleTest {

    private static final HashMap<Class<?>, Object> _extra = new HashMap<>();

    private GtfsModule builder;

    @Test
    public void testNoBikesByDefault() throws IOException {
        // We configure two trip: one with unknown bikes_allowed and the second with bikes
        // allowed.
        MockGtfs gtfs = getSimpleGtfs();
        gtfs.putTrips(2, "r0", "sid0", "bikes_allowed=0,1");
        gtfs.putStopTimes("t0,t1", "s0,s1");

        List<GtfsBundle> bundleList = getGtfsAsBundleList(gtfs);
        bundleList.get(0).setDefaultBikesAllowed(false);
        builder = new GtfsModule(bundleList);

        Graph graph = new Graph();
        builder.buildGraph(graph, _extra);
        graph.index(new DefaultStreetVertexIndexFactory());

        // Feed id is used instead of the agency id for OBA entities.
        GtfsBundle gtfsBundle = bundleList.get(0);
        GtfsFeedId feedId = gtfsBundle.getFeedId();

        Trip trip = graph.index.tripForId.get(new FeedScopedId(feedId.getId(), "t0"));
        TripPattern pattern = graph.index.patternForTrip.get(trip);
        List<Trip> trips = pattern.getTrips();
        assertEquals(BikeAccess.UNKNOWN,
                BikeAccess.fromTrip(withId(trips, new FeedScopedId(feedId.getId(), "t0"))));
        assertEquals(BikeAccess.ALLOWED,
                BikeAccess.fromTrip(withId(trips, new FeedScopedId(feedId.getId(), "t1"))));
    }

    @Test
    public void testBikesByDefault() throws IOException {
        // We configure two trip: one with unknown bikes_allowed and the second with no bikes
        // allowed.
        MockGtfs gtfs = getSimpleGtfs();
        gtfs.putTrips(2, "r0", "sid0", "bikes_allowed=0,2");
        gtfs.putStopTimes("t0,t1", "s0,s1");

        List<GtfsBundle> bundleList = getGtfsAsBundleList(gtfs);
        bundleList.get(0).setDefaultBikesAllowed(true);
        builder = new GtfsModule(bundleList);

        Graph graph = new Graph();
        builder.buildGraph(graph, _extra);
        graph.index(new DefaultStreetVertexIndexFactory());

        // Feed id is used instead of the agency id for OBA entities.
        GtfsBundle gtfsBundle = bundleList.get(0);
        GtfsFeedId feedId = gtfsBundle.getFeedId();

        Trip trip = graph.index.tripForId.get(new FeedScopedId(feedId.getId(), "t0"));
        TripPattern pattern = graph.index.patternForTrip.get(trip);
        List<Trip> trips = pattern.getTrips();
        assertEquals(BikeAccess.ALLOWED,
                BikeAccess.fromTrip(withId(trips, new FeedScopedId(feedId.getId(), "t0"))));
        assertEquals(BikeAccess.NOT_ALLOWED,
                BikeAccess.fromTrip(withId(trips, new FeedScopedId(feedId.getId(), "t1"))));
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

    private static List<GtfsBundle> getGtfsAsBundleList (MockGtfs gtfs) {
        GtfsBundle bundle = new GtfsBundle();
        bundle.setFeedId(new GtfsFeedId.Builder().id("FEED").build());
        bundle.setPath(gtfs.getPath());
        List<GtfsBundle> list = Lists.newArrayList();
        list.add(bundle);
        return list;
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
