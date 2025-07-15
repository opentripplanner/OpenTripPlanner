package org.opentripplanner.ext.flex;

import static graphql.Assert.assertTrue;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public final class FlexIntegrationTestData {

  private static final ResourceLoader RES = ResourceLoader.of(FlexIntegrationTestData.class);

  private static final File ASPEN_GTFS = RES.file("aspen-flex-on-demand.gtfs");
  static final File COBB_BUS_30_GTFS = RES.file("cobblinc-bus-30-only.gtfs.zip");
  static final File COBB_FLEX_GTFS = RES.file("cobblinc-scheduled-deviated-flex.gtfs");
  private static final File COBB_OSM = RES.file("cobb-county.filtered.osm.pbf");
  static final File MARTA_BUS_856_GTFS = RES.file("marta-bus-856-only.gtfs.zip");

  public static TestOtpModel aspenGtfs() {
    return buildFlexGraph(ASPEN_GTFS);
  }

  public static TestOtpModel cobbFlexGtfs() {
    return buildFlexGraph(COBB_FLEX_GTFS);
  }

  public static TestOtpModel cobbOsm() {
    return ConstantsForTests.buildOsmGraph(COBB_OSM);
  }

  private static TestOtpModel buildFlexGraph(File file) {
    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);
    var timetableRepository = new TimetableRepository(new SiteRepository(), deduplicator);
    GtfsBundle gtfsBundle = GtfsBundle.forTest(file);
    GtfsModule module = GtfsModule.forTest(
      List.of(gtfsBundle),
      timetableRepository,
      graph,
      new ServiceDateInterval(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))
    );
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
    module.buildGraph();
    timetableRepository.index();
    graph.index();
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
    assertTrue(timetableRepository.hasFlexTrips());
    return new TestOtpModel(graph, timetableRepository);
  }
}
