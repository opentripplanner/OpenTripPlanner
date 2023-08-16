package org.opentripplanner.ext.flex;

import static graphql.Assert.assertTrue;
import static org.opentripplanner.test.support.ResourceLoader.file;

import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public abstract class FlexTest {

  protected static final File ASPEN_GTFS = file("/flex/aspen-flex-on-demand.gtfs.zip");
  protected static final File COBB_FLEX_GTFS = file(
    "/flex/cobblinc-scheduled-deviated-flex.gtfs.zip"
  );
  protected static final File COBB_BUS_30_GTFS = file("/flex/cobblinc-bus-30-only.gtfs.zip");
  protected static final File MARTA_BUS_856_GTFS = file("/flex/marta-bus-856-only.gtfs.zip");
  protected static final File LINCOLN_COUNTY_GBFS = file("/flex/lincoln-county-flex.gtfs.zip");
  protected static final File COBB_OSM = file("/flex/cobb-county.filtered.osm.pbf");

  protected static final DirectFlexPathCalculator calculator = new DirectFlexPathCalculator();
  protected static final LocalDate serviceDate = LocalDate.of(2021, 4, 11);
  protected static final int secondsSinceMidnight = LocalTime.of(10, 0).toSecondOfDay();
  protected static final FlexServiceDate flexDate = new FlexServiceDate(
    serviceDate,
    secondsSinceMidnight,
    new TIntHashSet()
  );

  protected static TestOtpModel buildFlexGraph(File file) {
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
