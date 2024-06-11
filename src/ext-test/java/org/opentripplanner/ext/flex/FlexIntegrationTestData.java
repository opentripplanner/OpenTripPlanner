package org.opentripplanner.ext.flex;

import static graphql.Assert.assertTrue;

import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexServiceDate;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfo;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public final class FlexIntegrationTestData {

  private static final ResourceLoader RES = ResourceLoader.of(FlexIntegrationTestData.class);

  private static final File ASPEN_GTFS = RES.file("aspen-flex-on-demand.gtfs");
  static final File COBB_BUS_30_GTFS = RES.file("cobblinc-bus-30-only.gtfs.zip");
  static final File COBB_FLEX_GTFS = RES.file("cobblinc-scheduled-deviated-flex.gtfs");
  private static final File COBB_OSM = RES.file("cobb-county.filtered.osm.pbf");
  private static final File LINCOLN_COUNTY_GTFS = RES.file("lincoln-county-flex.gtfs");
  static final File MARTA_BUS_856_GTFS = RES.file("marta-bus-856-only.gtfs.zip");

  public static final DirectFlexPathCalculator CALCULATOR = new DirectFlexPathCalculator();
  private static final LocalDate SERVICE_DATE = LocalDate.of(2021, 4, 11);
  private static final int SECONDS_SINCE_MIDNIGHT = LocalTime.of(10, 0).toSecondOfDay();
  public static final FlexServiceDate FLEX_DATE = new FlexServiceDate(
    SERVICE_DATE,
    SECONDS_SINCE_MIDNIGHT,
    RoutingBookingInfo.NOT_SET,
    new TIntHashSet()
  );

  public static TestOtpModel aspenGtfs() {
    return buildFlexGraph(ASPEN_GTFS);
  }

  public static TestOtpModel cobbFlexGtfs() {
    return buildFlexGraph(COBB_FLEX_GTFS);
  }

  public static TestOtpModel cobbBus30Gtfs() {
    return buildFlexGraph(COBB_BUS_30_GTFS);
  }

  public static TestOtpModel martaBus856Gtfs() {
    return buildFlexGraph(MARTA_BUS_856_GTFS);
  }

  public static TestOtpModel lincolnCountyGtfs() {
    return buildFlexGraph(LINCOLN_COUNTY_GTFS);
  }

  public static TestOtpModel cobbOsm() {
    return ConstantsForTests.buildOsmGraph(COBB_OSM);
  }

  private static TestOtpModel buildFlexGraph(File file) {
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
