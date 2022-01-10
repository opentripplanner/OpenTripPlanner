package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.plan.PlaceTest;
import org.opentripplanner.graph_builder.model.MinTimeTransfer;
import org.opentripplanner.routing.graph.Graph;

class ApplyMinTimeTransfersTest {

    ApplyMinTimeTransfers module = new ApplyMinTimeTransfers();
    Stop stop1 = PlaceTest.stop("1", 1, 1);
    Stop stop2 = PlaceTest.stop("2", 2, 2);
    Stop stop3 = PlaceTest.stop("3", 3, 3);

    @Test
    public void shouldOverrideDistanceOfExistingTransfer() {
        var issueStore = new DataImportIssueStore(true);
        var graph = new Graph();
        var transfer1 = new PathTransfer(stop1, stop2, Set.of(BIKE), 10, List.of());
        var transfer2 = new PathTransfer(stop2, stop1, Set.of(WALK), 10, List.of());
        var transfer3 = new PathTransfer(stop3, stop1, Set.of(WALK), 10, List.of());
        var transfer4 = new PathTransfer(stop1, stop2, Set.of(WALK, BIKE), 50, List.of());

        graph.transfersByStop.put(stop1, transfer1);
        graph.transfersByStop.put(stop1, transfer4);
        graph.transfersByStop.put(stop2, transfer2);
        graph.transfersByStop.put(stop3, transfer3);

        HashMap<Class<?>, Object> extra = new HashMap<>();
        var transfers = List.of(
                new MinTimeTransfer(stop1, stop2, Duration.ofSeconds(140)),
                new MinTimeTransfer(stop2, stop1, Duration.ofSeconds(280)),
                new MinTimeTransfer(stop3, stop1, Duration.ofSeconds(500))
        );
        extra.put(MinTimeTransfer.class, transfers);

        module.buildGraph(graph, extra, issueStore);

        assertEquals(0, issueStore.getIssues().size());

        var stop1Transfers = new ArrayList<>(graph.transfersByStop.get(stop1));

        assertEquals(2, stop1Transfers.size());

        // we have both a BIKE and WALK,BIKE transfer and expect the latter one to be modified
        var adjustedTransfer = stop1Transfers.stream().filter(t -> t.hasMode(WALK)).findFirst().get();
        assertEquals(100, adjustedTransfer.getDistanceMeters());
        assertEquals(List.of(), adjustedTransfer.getEdges());
    }

    @Test
    public void shouldAddNewTransferWhenNotAlreadyInGraph() {
        var issueStore = new DataImportIssueStore(true);
        var graph = new Graph();

        HashMap<Class<?>, Object> extra = new HashMap<>();
        var transfers = List.of(new MinTimeTransfer(stop1, stop2, Duration.ofMinutes(10)));
        extra.put(MinTimeTransfer.class, transfers);

        module.buildGraph(graph, extra, issueStore);

        assertEquals(1, issueStore.getIssues().size());
        assertEquals("NoTransferStreetPath", issueStore.getIssues().get(0).getType());
    }
}