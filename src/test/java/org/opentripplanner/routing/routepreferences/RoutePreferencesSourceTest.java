package org.opentripplanner.routing.routepreferences;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import com.google.common.collect.Lists;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;

import java.io.File;


public class RoutePreferencesSourceTest {
    @Test
    public void testFromConfig() {
        String config = "HSL";
        RoutePreferencesSource source = RoutePreferencesSource.fromConfig(config);
        assertTrue(source instanceof HSLRoutePreferencesSource);
    }

    @Test
    public void testSetRoutePreferences() {
        GraphBuilder graphBuilder = new GraphBuilder();
        List<GtfsBundle> gtfsBundles = Lists.newArrayList();
        GtfsBundle gtfsBundle = new GtfsBundle(new File(ConstantsForTests.HSL_MINIMAL_GTFS));
        gtfsBundles.add(gtfsBundle);
        GtfsModule gtfsModule = new GtfsModule(gtfsBundles);
        graphBuilder.addModule(gtfsModule);
        graphBuilder.serializeGraph = false;
        graphBuilder.run();

        Graph graph = graphBuilder.getGraph();

        graph.index(new DefaultStreetVertexIndexFactory());

        String config = "HSL";
        RoutePreferencesSource source = RoutePreferencesSource.fromConfig(config);

        RoutingRequest options = new RoutingRequest();
        // Goes through routes and sets some of them as unpreferred routes based on regex patterns
        source.setRoutePreferences(options, graph);

        RouteMatcher unpreferredRoutes = options.unpreferredRoutes;

        Route route2143 = new Route();
        route2143.setId(new FeedScopedId("HSL", "2143"));
        assertFalse(unpreferredRoutes.matches(route2143));

        Route route2143A = new Route();
        route2143A.setId(new FeedScopedId("HSL", "2143A"));
        assertTrue(unpreferredRoutes.matches(route2143A));

        Route route2146N = new Route();
        route2146N.setId(new FeedScopedId("HSL", "2146N"));
        assertFalse(unpreferredRoutes.matches(route2146N));

        Route route2164VA= new Route();
        route2164VA.setId(new FeedScopedId("HSL", "2164VA"));
        assertTrue(unpreferredRoutes.matches(route2164VA));

        Route route7848= new Route();
        route7848.setId(new FeedScopedId("HSL", "7848"));
        assertTrue(unpreferredRoutes.matches(route7848));
    }
}