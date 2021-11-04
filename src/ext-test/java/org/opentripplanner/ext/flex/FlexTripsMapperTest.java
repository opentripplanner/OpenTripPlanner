package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;

public class FlexTripsMapperTest {

    static final String ASPEN_GTFS = "src/ext-test/resources/aspen-flex-on-demand.gtfs.zip";
    static final DirectFlexPathCalculator calculator = new DirectFlexPathCalculator(null);
    static final ServiceDate serviceDate = new ServiceDate(2021, 4, 11);
    static final int secondsSinceMidnight = LocalTime.of(10, 0).toSecondOfDay();

    @Test
    public void parseAspenTaxiAsUnscheduledTrip() {
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

        var trip = flexTrips.iterator().next();

        var stopLocation = trip.getStops().iterator().next();
        var nearbyStop = new NearbyStop(stopLocation, 0, List.of(), null, null);
        var flexDate = new FlexServiceDate(serviceDate, secondsSinceMidnight, new TIntHashSet());

        var accesses = trip.getFlexAccessTemplates(
                nearbyStop,
                flexDate,
                calculator
        ).collect(Collectors.toList());

        assertEquals(1, accesses.size());

        var access = accesses.get(0);
        assertEquals(0, access.fromStopIndex);
        assertEquals(0, access.toStopIndex);

        var egresses = trip.getFlexEgressTemplates(
                nearbyStop,
                flexDate,
                calculator
        ).collect(Collectors.toList());

        assertEquals(1, egresses.size());

        var egress = egresses.get(0);
        assertEquals(0, egress.fromStopIndex);
        assertEquals(0, egress.toStopIndex);

    }
}