package org.opentripplanner.ext.flex;

import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.OTPFeature;

abstract public class FlexTest {

    static final DirectFlexPathCalculator calculator = new DirectFlexPathCalculator(null);
    static final ServiceDate serviceDate = new ServiceDate(2021, 4, 11);
    static final int secondsSinceMidnight = LocalTime.of(10, 0).toSecondOfDay();
    static final FlexServiceDate flexDate =
            new FlexServiceDate(serviceDate, secondsSinceMidnight, new TIntHashSet());
    static final FlexParameters params = new FlexParameters(300);


    static Graph buildFlexGraph(String fileName) {
        var graph = new Graph();
        GtfsBundle gtfsBundle = new GtfsBundle(new File(fileName));
        GtfsModule module = new GtfsModule(
                List.of(gtfsBundle),
                new ServiceDateInterval(
                        new ServiceDate(2021, 1, 1),
                        new ServiceDate(2022, 1, 1)
                )
        );
        OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
        module.buildGraph(graph, new HashMap<>());
        OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
        return graph;
    }

}
