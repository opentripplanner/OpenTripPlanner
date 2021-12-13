package org.opentripplanner.ext.flex;

import static graphql.Assert.assertFalse;

import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.OTPFeature;

abstract public class FlexTest {

    static final String ASPEN_GTFS = "/flex/aspen-flex-on-demand.gtfs.zip";
    static final String COBB_FLEX_GTFS = "/flex/cobblinc-scheduled-deviated-flex.gtfs.zip";
    static final String COBB_BUS_30_GTFS = "/flex/cobblinc-bus-30-only.gtfs.zip";
    static final String MARTA_BUS_856_GTFS = "/flex/marta-bus-856-only.gtfs.zip";
    static final String LINCOLN_COUNTY_GBFS = "/flex/lincoln-county-flex.gtfs.zip";
    static final String COBB_OSM = "/flex/cobb-county.filtered.osm.pbf";

    static final DirectFlexPathCalculator calculator = new DirectFlexPathCalculator(null);
    static final ServiceDate serviceDate = new ServiceDate(2021, 4, 11);
    static final int secondsSinceMidnight = LocalTime.of(10, 0).toSecondOfDay();
    static final FlexServiceDate flexDate =
            new FlexServiceDate(serviceDate, secondsSinceMidnight, new TIntHashSet());
    static final FlexParameters params = new FlexParameters(300);


    static Graph buildFlexGraph(String fileName) {
        File file = null;
        try {
            file = FakeGraph.getFileForResource(fileName);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        var graph = new Graph();
        GtfsBundle gtfsBundle = new GtfsBundle(file);
        GtfsModule module = new GtfsModule(
                List.of(gtfsBundle),
                new ServiceDateInterval(
                        new ServiceDate(2021, 1, 1),
                        new ServiceDate(2022, 1, 1)
                )
        );
        OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
        module.buildGraph(graph, new HashMap<>());
        graph.index();
        OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
        assertFalse(graph.flexTripsById.isEmpty());
        return graph;
    }

}
