package org.opentripplanner.routing.graphfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.graphfinder.DirectGraphFinderTest.linestring;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

class StreetGraphFinderTest extends GraphRoutingTest {

    private TransitStopVertex S1, S2, S3;
    private IntersectionVertex A, B, C, D;
    private BikeRentalStationVertex BR1, BR2;
    private BikeParkVertex BP1;
    private ParkAndRideVertex PR1, PR2;
    private RoutingService routingService;
    private StreetGraphFinder graphFinder;
    private Route R1, R2;
    private TripPattern TP1, TP2;

    @BeforeEach
    protected void setUp() throws Exception {
        var a = new Agency(new FeedScopedId("F", "Agency"), "Agency", null);

        R1 = new Route(new FeedScopedId("F", "R1"));
        R1.setAgency(a);
        R1.setMode(TransitMode.BUS);

        R2 = new Route(new FeedScopedId("F", "R2"));
        R2.setAgency(a);
        R2.setMode(TransitMode.TRAM);

        var graph = graphOf(new Builder() {
            @Override
            public void build() {
                S1 = stop("S1", 47.500, 19.001);
                S2 = stop("S2", 47.510, 19.001);
                S3 = stop("S3", 47.520, 19.001);

                BR1 = bikeRentalStation("BR1", 47.500, 18.999);
                BR2 = bikeRentalStation("BR2", 47.520, 18.999);

                BP1 = bikePark("BP1", 47.520, 18.999);

                PR1 = carPark("PR1", 47.510, 18.999);
                PR2 = carPark("PR2", 47.530, 18.999);

                A = intersection("A", 47.500, 19.00);
                B = intersection("B", 47.510, 19.00);
                C = intersection("C", 47.520, 19.00);
                D = intersection("D", 47.530, 19.00);

                biLink(A, S1);
                biLink(A, BR1);
                biLink(B, S2);
                biLink(B, PR1);
                biLink(C, S3);
                biLink(C, BP1);
                biLink(C, BR2);
                biLink(D, PR2);

                street(A, B, 100, StreetTraversalPermission.ALL);
                street(B, C, 100, StreetTraversalPermission.ALL);
                street(C, D, 100, StreetTraversalPermission.ALL);

                tripPattern(TP1 = new TripPattern(new FeedScopedId("F", "TP1"), R1,
                        new StopPattern(List.of(st(S1), st(S2)))
                ));
                tripPattern(TP2 = new TripPattern(new FeedScopedId("F", "TP2"), R2,
                        new StopPattern(List.of(st(S1), st(S3)))
                ));
            }
        });

        graph.index = new GraphIndex(graph);

        routingService = new RoutingService(graph);
        graphFinder = new StreetGraphFinder(graph);
    }

    @Test
    void findClosestStops() {
        var ns1 = new NearbyStop(S1, 0, null, linestring(47.500, 19.000, 47.500, 19.001), null);
        var ns2 = new NearbyStop(S2, 100, null, linestring(47.500, 19.000, 47.510, 19.000, 47.510, 19.001), null);

        assertEquals(
                List.of(ns1),
                simplify(graphFinder.findClosestStops(47.500, 19.000, 10))
        );

        assertEquals(
                List.of(ns1, ns2),
                simplify(graphFinder.findClosestStops(47.500, 19.000, 100))
        );
    }

    @Test
    void findClosestPlacesLimiting() {
        var ns1 = new PlaceAtDistance(S1.getStop(), 0);
        var ns2 = new PlaceAtDistance(S2.getStop(), 100);
        var ns3 = new PlaceAtDistance(S3.getStop(), 200);
        var br1 = new PlaceAtDistance(BR1.getStation(), 0);
        var br2 = new PlaceAtDistance(BR2.getStation(), 200);
        var ps11 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP1), 0);
        var ps21 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP2), 0);

        assertEquals(
                List.of(ns1, ps21, ps11, br1),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 10.0, 10,
                        null, null, null, null, null, null, null,
                        routingService
                )
        );

        assertEquals(
                List.of(ns1, ps21, ps11, br1, ns2, ns3, br2),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 200.0, 100,
                        null, null, null, null, null, null, null,
                        routingService
                )
        );

        assertEquals(
                List.of(ns1, ps21, ps11),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 200.0, 3,
                        null, null, null, null, null, null, null,
                        routingService
                )
        );
    }

    @Test
    void findClosestPlacesWithAModeFilter() {
        var ns1 = new PlaceAtDistance(S1.getStop(), 0);
        var ns2 = new PlaceAtDistance(S2.getStop(), 100);
        var ns3 = new PlaceAtDistance(S3.getStop(), 200);

        assertEquals(
                List.of(ns1, ns2, ns3),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 200.0, 100,
                        null, List.of(PlaceType.STOP), null, null, null, null, null,
                        routingService
                )
        );

        assertEquals(
                List.of(ns1, ns2),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 200.0, 100,
                        List.of(TransitMode.BUS), List.of(PlaceType.STOP), null, null, null, null, null,
                        routingService
                )
        );
    }

    @Test
    void findClosestPlacesWithAStopFilter() {
        var ns1 = new PlaceAtDistance(S1.getStop(), 0);
        var ns2 = new PlaceAtDistance(S2.getStop(), 100);
        var ps11 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP1), 0);
        var ps21 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP2), 0);

        assertEquals(
                List.of(ns1, ps21, ps11, ns2),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 100.0, 100,
                        null, List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP), null, null, null, null, null,
                        routingService
                )
        );

        assertEquals(
                List.of(ps21, ps11, ns2),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 100.0, 100,
                        null, List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP), List.of(S2.getStop().getId()), null, null, null, null,
                        routingService
                )
        );
    }

    @Test
    void findClosestPlacesWithAStopAndRouteFilter() {
        var ns1 = new PlaceAtDistance(S1.getStop(), 0);
        var ns2 = new PlaceAtDistance(S2.getStop(), 100);
        var ps11 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP1), 0);
        var ps21 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP2), 0);

        assertEquals(
                List.of(ns1, ps21, ps11, ns2),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 100.0, 100,
                        null, List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP), null, null, null, null, null,
                        routingService
                )
        );

        assertEquals(
                List.of(ps11, ns2),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 100.0, 100,
                        null, List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP), List.of(S2.getStop().getId()), List.of(R1.getId()), null, null, null,
                        routingService
                )
        );
    }

    @Test
    void findClosestPlacesWithARouteFilter() {
        var ns1 = new PlaceAtDistance(S1.getStop(), 0);
        var ns2 = new PlaceAtDistance(S2.getStop(), 100);
        var ns3 = new PlaceAtDistance(S3.getStop(), 200);
        var ps11 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP1), 0);
        var ps21 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP2), 0);

        assertEquals(
                List.of(ns1, ps21, ps11, ns2, ns3),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 200.0, 100,
                        null, List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP), null, null, null, null, null,
                        routingService
                )
        );

        assertEquals(
                List.of(ns1, ps21, ns2, ns3),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 200.0, 100,
                        null, List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP), null, List.of(R2.getId()), null, null, null,
                        routingService
                )
        );
    }

    @Test
    void findClosestPlacesWithABikeRentalFilter() {
        var br1 = new PlaceAtDistance(BR1.getStation(), 0);
        var br2 = new PlaceAtDistance(BR2.getStation(), 200);

        assertEquals(
                List.of(br1, br2),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 300.0, 100,
                        null, List.of(PlaceType.BICYCLE_RENT), null, null, null, null, null,
                        routingService
                )
        );

        assertEquals(
                List.of(br2),
                graphFinder.findClosestPlaces(
                        47.500, 19.000, 300.0, 100,
                        null, List.of(PlaceType.BICYCLE_RENT), null, null, List.of("BR2"), null, null,
                        routingService
                )
        );
    }

    @Test
    @Disabled
    void findClosestPlacesWithABikeParkFilter() {
        // This is not implemented in StreetGraphFinder
    }

    @Test
    @Disabled
    void findClosestPlacesWithACarParkFilter() {
        // This is not implemented in StreetGraphFinder
    }

    private List<NearbyStop> simplify(List<NearbyStop> closestStops) {
        return closestStops.stream().map(
                ns -> new NearbyStop(
                        ns.stop, ns.distance, ns.distanceIndependentTime, null, ns.geometry, null
                )
        )
                .collect(Collectors.toList());
    }

    private StopTime st(TransitStopVertex s1) {
        var st = new StopTime();
        st.setStop(s1.getStop());
        return st;
    }
}