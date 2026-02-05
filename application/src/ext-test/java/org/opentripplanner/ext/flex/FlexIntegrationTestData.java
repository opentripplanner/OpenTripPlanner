package org.opentripplanner.ext.flex;

import static graphql.Assert.assertTrue;

import java.io.File;
import java.util.List;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.core.model.time.LocalDateInterval;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundleTestFactory;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.gtfs.graphbuilder.GtfsModuleTestFactory;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public final class FlexIntegrationTestData {

  private static final ResourceLoader RES = ResourceLoader.of(FlexIntegrationTestData.class);

  private static final File ASPEN_GTFS = RES.file("aspen-flex-on-demand.gtfs");
  static final File COBB_BUS_30_GTFS = RES.file("cobblinc-bus-30-only.gtfs.zip");
  static final File COBB_FLEX_GTFS = RES.file("cobblinc-scheduled-deviated-flex.gtfs");
  static final File COBB_OSM = RES.file("cobb-county.filtered.osm.pbf");
  static final File MARTA_BUS_856_GTFS = RES.file("marta-bus-856-only.gtfs.zip");
  static final File MIDNIGHT_FLEX_GTFS = RES.file("midnight-flex.gtfs");

  public static TestOtpModel aspenGtfs() {
    return buildFlexOnlyGraph(ASPEN_GTFS);
  }

  public static TestOtpModel cobbFlexGtfs() {
    return buildFlexOnlyGraph(COBB_FLEX_GTFS);
  }

  public static TestOtpModel cobbOsm() {
    return ConstantsForTests.buildOsmGraph(COBB_OSM);
  }

  public static TestOtpModel midnightFlexGtfs() {
    return buildFlexOnlyGraph(MIDNIGHT_FLEX_GTFS);
  }

  static TestOtpModel buildFlexOnlyGraph(File file) {
    var graph = new Graph();
    var timetableRepository = new TimetableRepository(new SiteRepository());
    GtfsBundle gtfsBundle = GtfsBundleTestFactory.forTest(file);
    GtfsModule module = GtfsModuleTestFactory.forTest(
      List.of(gtfsBundle),
      timetableRepository,
      graph,
      LocalDateInterval.unbounded()
    );
    OTPFeature.FlexRouting.testOn(() -> {
      module.buildGraph();
      timetableRepository.index();
      graph.index();
    });
    assertTrue(timetableRepository.hasFlexTrips());
    return new TestOtpModel(graph, timetableRepository, TransferServiceTestFactory.withFlex());
  }
}
