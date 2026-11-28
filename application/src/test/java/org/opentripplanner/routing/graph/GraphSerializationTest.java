package org.opentripplanner.routing.graph;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.ext.fares.service.gtfs.v1.GtfsFareServiceFactory;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.shape.dataimport.ShapeDataImport;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.internal.DefaultStreetRepository;
import org.opentripplanner.street.model.StreetModelDetails;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;

/**
 * A minimal test that a graph survives a round trip through {@link SerializedGraphObject#save} and
 * {@link SerializedGraphObject#load(File)}. The test compares the street graph and the scheduled
 * transit data, it does not compare the object graph field by field.
 * <p>
 * That all classes in the serialized object graph can be loaded is verified by
 * {@code SerializedGraphObjectClassesTest}, and each custom Kryo serializer has its own test.
 */
class GraphSerializationTest {

  @TempDir
  File tempDir;

  @Test
  void roundTripGtfsGraph() {
    var model = ConstantsForTests.buildNewPortlandGraph(true);
    // Make sure there are shapes, otherwise the shape comparison proves nothing
    var shapes = model
      .timetableDataImport()
      .getAllTripPatterns()
      .stream()
      .map(p -> model.shapeDataImport().getTripPatternShape(p.getId()))
      .filter(Objects::nonNull)
      .toList();
    assertThat(shapes).isNotEmpty();
    assertRoundTrip(model);
  }

  @Test
  void roundTripNetexGraph() {
    assertRoundTrip(ConstantsForTests.buildNewMinimalNetexGraph());
  }

  private void assertRoundTrip(TestOtpModel model) {
    var file = new File(tempDir, "graph.obj");
    save(model, file);

    // The model is cached and may not be indexed by other tests
    model.index();

    var copy = SerializedGraphObject.load(file);
    copy.graph.index();

    assertSameStreetGraph(model.graph(), copy.graph);
    assertSameScheduledData(model.timetableDataImport(), copy.timetableDataImport);
    assertSameShapeData(model.timetableDataImport(), model.shapeDataImport(), copy.shapeDataImport);
  }

  private static void save(TestOtpModel model, File file) {
    var streetRepository = new DefaultStreetRepository();
    streetRepository.setStreetModelDetails(new StreetModelDetails(33f, 17, 1.0f, 1.0f));

    new SerializedGraphObject(
      model.graph(),
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultStreetDetailsRepository(),
      streetRepository,
      model.transitRepository(),
      model.timetableDataImport(),
      model.shapeDataImport(),
      model.transferRepository(),
      RegularTransferFactory.createBuildRepository(),
      new DefaultWorldEnvelopeRepository(),
      new DefaultVehicleParkingRepository(),
      BuildConfig.DEFAULT,
      RouterConfig.DEFAULT,
      DataImportIssueSummary.empty(),
      null,
      null,
      null,
      null,
      new GtfsFareServiceFactory()
    ).save(new FileDataSource(file, FileType.GRAPH));
  }

  /**
   * Vertices are compared by label and location, and edges by their description (which includes
   * the vertices, name and OSM ids) and geometry. The edge lists of the vertices are rebuilt after
   * loading, and their order is not significant, so they are not compared directly.
   */
  private static void assertSameStreetGraph(Graph expected, Graph actual) {
    assertEquals(expected.countVertices(), actual.countVertices());
    assertEquals(expected.countEdges(), actual.countEdges());
    assertEquals(vertices(expected), vertices(actual));
    assertEquals(edges(expected), edges(actual));
  }

  private static Map<String, String> vertices(Graph graph) {
    var result = new TreeMap<String, String>();
    for (var v : graph.getVertices()) {
      result.put(v.getLabelString(), v.getLat() + "," + v.getLon());
    }
    return result;
  }

  private static Set<String> edges(Graph graph) {
    var result = new TreeSet<String>();
    for (var e : graph.listEdges()) {
      result.add(e + " " + (e.hasGeometry() ? e.getGeometry().toText() : ""));
    }
    return result;
  }

  /**
   * The scheduled entities are identity based, so compare their ids, the order of the trips on
   * service date (it is the order of replacements) and the calendars.
   */
  private static void assertSameScheduledData(
    TimetableDataImport original,
    TimetableDataImport copy
  ) {
    assertEquals(
      original.getAllTripPatterns().stream().map(TripPattern::getId).collect(Collectors.toSet()),
      copy.getAllTripPatterns().stream().map(TripPattern::getId).collect(Collectors.toSet())
    );
    assertEquals(
      original.getAllTripsOnServiceDate().stream().map(TripOnServiceDate::getId).toList(),
      copy.getAllTripsOnServiceDate().stream().map(TripOnServiceDate::getId).toList()
    );
    var originalCalendars = original.getTripCalendars();
    var copyCalendars = copy.getTripCalendars();
    assertEquals(originalCalendars.listServiceIds(), copyCalendars.listServiceIds());
    for (var serviceId : originalCalendars.listServiceIds()) {
      assertEquals(
        originalCalendars.listServiceDates(serviceId),
        copyCalendars.listServiceDates(serviceId)
      );
    }
    assertEquals(original.getServiceCodes(), copy.getServiceCodes());
  }

  private static void assertSameShapeData(
    TimetableDataImport timetableDataImport,
    ShapeDataImport original,
    ShapeDataImport copy
  ) {
    for (var pattern : timetableDataImport.getAllTripPatterns()) {
      var originalShape = original.getTripPatternShape(pattern.getId());
      if (originalShape == null) {
        continue;
      }
      var copiedShape = copy.getTripPatternShape(pattern.getId());
      assertEquals(originalShape.size(), copiedShape.size());
      for (int i = 0; i < originalShape.size(); i++) {
        assertEquals(originalShape.get(i), copiedShape.get(i));
        assertEquals(
          originalShape.distanceBetween(i, i + 1),
          copiedShape.distanceBetween(i, i + 1)
        );
      }
    }
  }
}
