package org.opentripplanner.ext.flex;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.OTPFeature;

public class FlexTripsMapperTest {

    public static final String ASPEN_GTFS = "src/ext-test/resources/aspen-flex-on-demand.gtfs.zip";

    @Test
    public void parseAspenOnDemandTaxi() {
        OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
        Graph graph = new Graph();

        GtfsBundle gtfsBundle = new GtfsBundle(new File(ASPEN_GTFS));
        GtfsModule module = new GtfsModule(
                List.of(gtfsBundle),
                new ServiceDateInterval(new ServiceDate(2021, 1, 1), new ServiceDate(2022, 1, 1)),
                true
        );
        module.buildGraph(graph, new HashMap<>());

        System.out.println(graph.flexTripsById);
        Assertions.assertFalse(graph.flexTripsById.isEmpty());
    }
}