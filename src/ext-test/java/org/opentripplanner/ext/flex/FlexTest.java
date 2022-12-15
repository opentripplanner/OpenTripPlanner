package org.opentripplanner.ext.flex;

import static graphql.Assert.assertTrue;

import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public abstract class FlexTest {

  static final String ASPEN_GTFS = "/flex/aspen-flex-on-demand.gtfs.zip";
  static final String COBB_FLEX_GTFS = "/flex/cobblinc-scheduled-deviated-flex.gtfs.zip";
  static final String COBB_BUS_30_GTFS = "/flex/cobblinc-bus-30-only.gtfs.zip";
  static final String MARTA_BUS_856_GTFS = "/flex/marta-bus-856-only.gtfs.zip";
  static final String LINCOLN_COUNTY_GBFS = "/flex/lincoln-county-flex.gtfs.zip";
  static final String COBB_OSM = "/flex/cobb-county.filtered.osm.pbf";

  static final DirectFlexPathCalculator calculator = new DirectFlexPathCalculator();
  static final LocalDate serviceDate = LocalDate.of(2021, 4, 11);
  static final int secondsSinceMidnight = LocalTime.of(10, 0).toSecondOfDay();
  static final FlexServiceDate flexDate = new FlexServiceDate(
    serviceDate,
    secondsSinceMidnight,
    new TIntHashSet()
  );

  static TestOtpModel buildFlexGraph(String fileName) {
    File file = null;
    try {
      file = FakeGraph.getFileForResource(fileName);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);
    var transitModel = new TransitModel(new StopModel(), deduplicator);
    GtfsBundle gtfsBundle = new GtfsBundle(file);
    GtfsModule module = new GtfsModule(
      List.of(gtfsBundle),
      transitModel,
      graph,
      new ServiceDateInterval(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))
    );
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
    module.buildGraph();
    transitModel.index();
    graph.index(transitModel.getStopModel());
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
    assertTrue(transitModel.hasFlexTrips());
    return new TestOtpModel(graph, transitModel);
  }
}
