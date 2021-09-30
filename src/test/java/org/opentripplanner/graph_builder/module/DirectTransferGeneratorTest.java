package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

/**
 * This creates a graph with trip patterns S0 - V0 | S11 - V11 --------> V21 - S21 |     \ S12 - V12
 * ----> V22 - V22
 */
class DirectTransferGeneratorTest extends GraphRoutingTest {

    private static final double MAX_TRANSFER_DURATION = 3600;
    private TransitStopVertex S0, S11, S12, S21, S22;
    private StreetVertex V0, V11, V12, V21, V22;

    private Graph graph(boolean addPatterns) {
        return graphOf(new Builder() {
            @Override
            public void build() {
                S0 = stop("S0", 47.495, 19.001);
                S11 = stop("S11", 47.500, 19.001);
                S12 = stop("S12", 47.520, 19.001);
                S21 = stop("S21", 47.500, 19.011);
                S22 = stop("S22", 47.520, 19.011);

                V0 = intersection("V0", 47.495, 19.000);
                V11 = intersection("V11", 47.500, 19.000);
                V12 = intersection("V12", 47.510, 19.000);
                V21 = intersection("V21", 47.500, 19.010);
                V22 = intersection("V22", 47.510, 19.010);

                biLink(V0, S0);
                biLink(V11, S11);
                biLink(V12, S12);
                biLink(V21, S21);
                biLink(V22, S22);

                street(V0, V11, 100, StreetTraversalPermission.ALL);
                street(V0, V12, 200, StreetTraversalPermission.ALL);
                street(V0, V21, 100, StreetTraversalPermission.ALL);
                street(V0, V22, 200, StreetTraversalPermission.ALL);

                street(V11, V12, 100, StreetTraversalPermission.PEDESTRIAN);
                street(V21, V22, 100, StreetTraversalPermission.PEDESTRIAN);
                street(V11, V21, 100, StreetTraversalPermission.PEDESTRIAN);
                street(V11, V22, 110, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

                if (addPatterns) {
                    var agency = agency("Agency");

                    tripPattern(new TripPattern(
                            new FeedScopedId("Test", "TP1"),
                            route("R1", TransitMode.BUS, agency),
                            new StopPattern(List.of(st(S11), st(S12)))
                    ));

                    tripPattern(new TripPattern(
                            new FeedScopedId("Test", "TP2"),
                            route("R2", TransitMode.BUS, agency),
                            new StopPattern(List.of(st(S21), st(S22)))
                    ));
                }
            }
        });
    }

    @Test
    public void testDirectTransfersWithoutPatterns() {
        var generator = new DirectTransferGenerator(
                MAX_TRANSFER_DURATION,
                List.of(
                        new RoutingRequest(
                                new RequestModes(null, StreetMode.WALK, null, null, null)
                        )
                )
        );

        var graph = graph(false);
        graph.index();
        graph.hasStreets = false;

        generator.buildGraph(graph, null);

        assertTransfers(
                graph.transfersByStop
        );
    }

    @Test
    public void testDirectTransfersWithPatterns() {
        var generator = new DirectTransferGenerator(
                MAX_TRANSFER_DURATION,
                List.of(
                        new RoutingRequest(
                                new RequestModes(null, StreetMode.WALK, null, null, null)
                        )
                )
        );

        var graph = graph(true);
        graph.hasStreets = false;

        generator.buildGraph(graph, null);

        assertTransfers(
                graph.transfersByStop,
                tr(S0, 556, S11),
                tr(S0, 935, S21),
                tr(S11, 751, S21),
                tr(S12, 751, S22),
                tr(S21, 751, S11),
                tr(S22, 751, S12)
        );
    }

    @Test
    public void testSingleRequestWithoutPatterns() {
        var generator = new DirectTransferGenerator(
                MAX_TRANSFER_DURATION,
                List.of(
                        new RoutingRequest(
                                new RequestModes(null, StreetMode.WALK, null, null, null)
                        )
                )
        );

        var graph = graph(false);
        graph.hasStreets = true;

        generator.buildGraph(graph, null);

        assertTransfers(
                graph.transfersByStop
        );
    }

    @Test
    public void testSingleRequestWithPatterns() {
        var generator = new DirectTransferGenerator(
                MAX_TRANSFER_DURATION,
                List.of(
                        new RoutingRequest(
                                new RequestModes(null, StreetMode.WALK, null, null, null)
                        )
                )
        );

        var graph = graph(true);
        graph.hasStreets = true;

        generator.buildGraph(graph, null);

        assertTransfers(
                graph.transfersByStop,
                tr(S0, 100, List.of(V0, V11), S11),
                tr(S0, 100, List.of(V0, V21), S21),
                tr(S11, 100, List.of(V11, V21), S21)
        );
    }

    @Test
    public void testMultipleRequestsWithoutPatterns() {
        var generator = new DirectTransferGenerator(
                MAX_TRANSFER_DURATION,
                List.of(
                        new RoutingRequest(
                                new RequestModes(null, StreetMode.WALK, null, null, null)
                        ),
                        new RoutingRequest(
                                new RequestModes(null, StreetMode.BIKE, null, null, null)
                        )
                )
        );

        var graph = graph(false);
        graph.hasStreets = true;

        generator.buildGraph(graph, null);

        assertTransfers(
                graph.transfersByStop
        );
    }

    @Test
    public void testMultipleRequestsWithPatterns() {
        var generator = new DirectTransferGenerator(
                MAX_TRANSFER_DURATION,
                List.of(
                        new RoutingRequest(
                                new RequestModes(null, StreetMode.WALK, null, null, null)
                        ),
                        new RoutingRequest(
                                new RequestModes(null, StreetMode.BIKE, null, null, null)
                        )
                )
        );

        var graph = graph(true);
        graph.hasStreets = true;

        generator.buildGraph(graph, null);

        assertTransfers(
                graph.transfersByStop,
                tr(S0, 100, List.of(V0, V11), S11),
                tr(S0, 100, List.of(V0, V21), S21),
                tr(S11, 100, List.of(V11, V21), S21),
                tr(S11, 110, List.of(V11, V22), S22)
        );
    }

    private void assertTransfers(
            Multimap<StopLocation, PathTransfer> transfersByStop,
            TransferDescriptor... transfers
    ) {
        var matchedTransfers = new HashSet<PathTransfer>();
        var assertions = Stream.concat(
                Arrays.stream(transfers).map(td -> td.matcher(transfersByStop, matchedTransfers)),
                Stream.of(allTransfersMatched(transfersByStop, matchedTransfers))
        );

        assertAll(assertions);
    }

    private Executable allTransfersMatched(
            Multimap<StopLocation, PathTransfer> transfersByStop,
            Set<PathTransfer> matchedTransfers
    ) {
        return () -> {
            var missingTransfers = new HashSet<>(transfersByStop.values());
            missingTransfers.removeAll(matchedTransfers);

            assertEquals(Set.of(), missingTransfers, "All transfers matched");
        };
    }

    private TransferDescriptor tr(TransitStopVertex from, double distance, TransitStopVertex to) {
        return new TransferDescriptor(from, distance, to);
    }

    private TransferDescriptor tr(
            TransitStopVertex from,
            double distance,
            List<StreetVertex> vertices,
            TransitStopVertex to
    ) {
        return new TransferDescriptor(from, distance, vertices, to);
    }

    private static class TransferDescriptor {

        private final StopLocation from;
        private final StopLocation to;
        private final Double distanceMeters;
        private final List<StreetVertex> vertices;

        public TransferDescriptor(
                TransitStopVertex from,
                Double distanceMeters,
                TransitStopVertex to
        ) {
            this.from = from.getStop();
            this.distanceMeters = distanceMeters;
            this.vertices = null;
            this.to = to.getStop();
        }

        public TransferDescriptor(
                TransitStopVertex from,
                Double distanceMeters,
                List<StreetVertex> vertices,
                TransitStopVertex to
        ) {
            this.from = from.getStop();
            this.distanceMeters = distanceMeters;
            this.vertices = vertices;
            this.to = to.getStop();
        }

        boolean matches(PathTransfer transfer) {
            if (!Objects.equals(from, transfer.from) || !Objects.equals(to, transfer.to)) {
                return false;
            }

            if (vertices == null) {
                return distanceMeters == transfer.getDistanceMeters()
                        && transfer.getEdges() == null;
            }
            else {
                var transferVertices = transfer.getEdges().stream()
                        .map(Edge::getToVertex)
                        .filter(StreetVertex.class::isInstance)
                        .collect(Collectors.toList());

                return distanceMeters == transfer.getDistanceMeters() && Objects.equals(
                        vertices, transferVertices);
            }
        }

        @Override
        public String toString() {
            return ToStringBuilder.of(getClass())
                    .addObj("from", from)
                    .addObj("to", to)
                    .addNum("distanceMeters", distanceMeters)
                    .addCol("vertices", vertices)
                    .toString();
        }

        private Executable matcher(
                Multimap<StopLocation, PathTransfer> transfersByStop,
                Set<PathTransfer> matchedTransfers
        ) {
            return () -> {
                var matched = transfersByStop.values()
                        .stream()
                        .filter(this::matches)
                        .findFirst();

                if (matched.isPresent()) {
                    assertTrue(true, "Found transfer for " + this);
                    matchedTransfers.add(matched.get());
                }
                else {
                    fail("Missing transfer for " + this);
                }
            };
        }
    }
}