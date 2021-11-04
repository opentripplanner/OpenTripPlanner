package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;

public class FlexTripsMapperTest {

    public static final String ASPEN_GTFS = "src/ext-test/resources/aspen-flex-on-demand.gtfs.zip";

    @Test
    public void parseAspenOnDemandTaxi() {
        Graph graph = new Graph();

        GtfsBundle gtfsBundle = new GtfsBundle(new File(ASPEN_GTFS));
        GtfsModule module = new GtfsModule(
                List.of(gtfsBundle),
                new ServiceDateInterval(new ServiceDate(2021, 1, 1), new ServiceDate(2022, 1, 1)),
                true
        );
        module.buildGraph(graph, new HashMap<>());

        var flexTrips = graph.flexTripsById.values();
        assertFalse(flexTrips.isEmpty());
        assertEquals(
                Set.of("t_1289262_b_29084_tn_0", "t_1289257_b_28352_tn_0"),
                flexTrips.stream().map(FlexTrip::getId).map(FeedScopedId::getId).collect(
                        Collectors.toSet())
        );

        assertEquals(
                Set.of(UnscheduledTrip.class),
                flexTrips.stream().map(FlexTrip::getClass).collect(
                        Collectors.toSet())
        );
    }
}