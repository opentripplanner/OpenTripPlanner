package org.opentripplanner.graph_builder.module;

import com.beust.jcommander.internal.Lists;
import org.junit.Test;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.gtfs.MockGtfs;
import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
        builder = new GtfsModule(bundleList, ServiceDateInterval.unbounded());

        Graph graph = new Graph();
        builder.buildGraph(graph, _extra);
        graph.index();

        // Feed id is used instead of the agency id for OBA entities.
        GtfsBundle gtfsBundle = bundleList.get(0);
        GtfsFeedId feedId = gtfsBundle.getFeedId();

        Trip trip = graph.index.getTripForId().get(new FeedScopedId(feedId.getId(), "t0"));
        TripPattern pattern = graph.index.getPatternForTrip().get(trip);
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
        builder = new GtfsModule(bundleList, ServiceDateInterval.unbounded());

        Graph graph = new Graph();
        builder.buildGraph(graph, _extra);
        graph.index();

        // Feed id is used instead of the agency id for OBA entities.
        GtfsBundle gtfsBundle = bundleList.get(0);
        GtfsFeedId feedId = gtfsBundle.getFeedId();

        Trip trip = graph.index.getTripForId().get(new FeedScopedId(feedId.getId(), "t0"));
        TripPattern pattern = graph.index.getPatternForTrip().get(trip);
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
        GtfsBundle bundle = new GtfsBundle(gtfs.getPath());
        bundle.setFeedId(new GtfsFeedId.Builder().id("FEED").build());
        List<GtfsBundle> list = Lists.newArrayList();
        list.add(bundle);
        return list;
    }

    private static <T extends TransitEntity> T withId(Iterable<T> beans, FeedScopedId id) {
        for (T bean : beans) {
            if (bean.getId().equals(id)) {
                return bean;
            }
        }
        return null;
    }
}
