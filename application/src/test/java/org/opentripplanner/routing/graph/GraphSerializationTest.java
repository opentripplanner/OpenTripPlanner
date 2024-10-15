package org.opentripplanner.routing.graph;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.conveyal.object_differ.ObjectDiffer;
import java.io.File;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import org.geotools.util.WeakValueHashMap;
import org.jets3t.service.io.TempFile;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.TransitModel;

/**
 * Tests that saving a graph and reloading it (round trip through serialization and deserialization)
 * does not corrupt the graph, and yields exactly the same data.
 * <p>
 * We tried several existing libraries to perform the comparison but nothing did exactly what we
 * needed in a way that we could control precisely.
 * <p>
 * Created by abyrd on 2018-10-26
 */
public class GraphSerializationTest {

  static Class<?>[] IGNORED_CLASSES = Set
    .of(
      // Skip AtomicInteger, it does not implement equals/hashCode
      AtomicInteger.class,
      ThreadPoolExecutor.class,
      WeakValueHashMap.class,
      Method.class,
      JarFile.class,
      SoftReference.class,
      Class.class,
      org.slf4j.Logger.class,
      ch.qos.logback.classic.Logger.class,
      HashGridSpatialIndex.class,
      Deduplicator.class
    )
    .toArray(Class[]::new);

  /**
   * Tests GTFS based graph serialization to file.
   */
  @Test
  public void testRoundTripSerializationForGTFSGraph() throws Exception {
    TestOtpModel model = ConstantsForTests.buildNewPortlandGraph(true);
    var weRepo = new DefaultWorldEnvelopeRepository();
    var emissionsDataModel = new EmissionsDataModel();
    testRoundTrip(model.graph(), model.transitModel(), weRepo, emissionsDataModel);
  }

  /**
   * Tests Netex based graph serialization to file.
   */
  @Test
  public void testRoundTripSerializationForNetexGraph() throws Exception {
    TestOtpModel model = ConstantsForTests.buildNewMinimalNetexGraph();
    var worldEnvelopeRepository = new DefaultWorldEnvelopeRepository();
    var emissionsDataModel = new EmissionsDataModel();
    testRoundTrip(model.graph(), model.transitModel(), worldEnvelopeRepository, emissionsDataModel);
  }

  // Ideally we'd also test comparing two separate but identical complex graphs, built separately from the same inputs.
  // A test that builds the same graph twice will currently fail for the following reasons:
  // There is global state in Vertex.index and the feeds IDs that mean if you build the same graph twice the feed IDs
  // and vertex index numbers will all be unique. This is however evidence that the diff tool is detecting changes in
  // the graph contents. On the other hand deserializing the same graph twice or doing a round trip through
  // serialization and deserialization should produce identical graphs.

  /**
   * Test comparison of two references to the same graph. This should obviously yield no differences
   * at all, and allows us to perform a very deep comparison of almost the entire object graph
   * because there are no problems with lists being reordered, transient indexes being rebuilt, etc.
   * The ObjectDiffer supports such comparisons of identical objects with a special switch
   * specifically for testing.
   * <p>
   * This is as much a test of the ObjectDiffer itself as of OpenTripPlanner serialization. It is
   * situated here instead of in the same package as ObjectDiffer so it has access to the
   * OpenTripPlanner classes, which provide a suitably complex tangle of fields and references for
   * exercising all the differ's capabilities.
   */
  @Test
  public void compareGraphToItself() {
    TestOtpModel cachedPortlandGraph = ConstantsForTests
      .getInstance()
      .getCachedPortlandGraph()
      .index();
    Graph originalGraph = cachedPortlandGraph.graph();

    // We can exclude relatively few classes here, because the object trees are of course perfectly identical.
    // We do skip edge lists - otherwise we trigger a depth-first search of the graph causing a stack overflow.
    // We also skip some deeply buried weak-value hash maps, which refuse to tell you what their keys are.
    ObjectDiffer objectDiffer = new ObjectDiffer();
    objectDiffer.ignoreFields("incoming", "outgoing");
    objectDiffer.useEquals(BitSet.class, LineString.class, Polygon.class);
    // ThreadPoolExecutor contains a weak reference to a very deep chain of Finalizer instances.
    // Method instances usually are part of a proxy which are totally un-reflectable in Java 11.

    objectDiffer.ignoreClasses(IGNORED_CLASSES);
    // This setting is critical to perform a deep test of an object against itself.
    objectDiffer.enableComparingIdenticalObjects();
    objectDiffer.compareTwoObjects(originalGraph, originalGraph);
    objectDiffer.printSummary();
    assertFalse(objectDiffer.hasDifferences());
  }

  /**
   * Compare two separate essentially empty graphs.
   */
  @Test
  public void testEmptyGraphs() {
    Graph graph1 = new Graph();
    Graph graph2 = new Graph();
    assertNoDifferences(graph1, graph2);
  }

  private static void assertNoDifferences(Graph g1, Graph g2) {
    // Make some exclusions because some classes are inherently transient or contain unordered lists we can't yet compare.
    ObjectDiffer objectDiffer = new ObjectDiffer();
    // Skip incoming and outgoing edge lists. These are unordered lists which will not compare properly.
    // The edges themselves will be compared via another field, and the edge lists are reconstructed after deserialization.
    // Some tests re-build the graph which will result in build times different by as little as a few milliseconds.
    // Some transient fields are not relevant to routing, so are not restored after reloading the graph.
    // Other structures contain Maps with keys that have identity equality - these also cannot be compared yet.
    // We would need to apply a key extractor function to such maps, copying them into new maps.
    objectDiffer.ignoreFields(
      "calendarService",
      "incoming",
      "outgoing",
      "buildTime",
      "tripPatternForId",
      "transitLayer",
      "realtimeTransitLayer",
      "dateTime",
      "notesForEdge",
      "uniqueMatchers"
    );
    // Edges have very detailed String representation including lat/lon coordinates and OSM IDs. They should be unique.
    objectDiffer.setKeyExtractor("turnRestrictions", Object::toString);
    objectDiffer.useEquals(BitSet.class, LineString.class, Polygon.class);
    // HashGridSpatialIndex contains unordered lists in its bins. This is rebuilt after deserialization anyway.
    // The deduplicator in the loaded graph will be empty, because it is transient and only fills up when items
    // are deduplicated.
    objectDiffer.ignoreClasses(IGNORED_CLASSES);
    objectDiffer.compareTwoObjects(g1, g2);
    objectDiffer.printSummary();
    // Print differences before assertion so we can see what went wrong.
    assertFalse(objectDiffer.hasDifferences());
  }

  /**
   * Tests that saving a Graph to disk and reloading it results in a separate but semantically
   * identical Graph.
   */
  private void testRoundTrip(
    Graph originalGraph,
    TransitModel originalTransitModel,
    WorldEnvelopeRepository worldEnvelopeRepository,
    EmissionsDataModel emissionsDataModel
  ) throws Exception {
    // Now round-trip the graph through serialization.
    File tempFile = TempFile.createTempFile("graph", "pdx");
    var streetLimitationParameters = new StreetLimitationParameters();
    streetLimitationParameters.initMaxCarSpeed(40);
    SerializedGraphObject serializedObj = new SerializedGraphObject(
      originalGraph,
      originalTransitModel,
      worldEnvelopeRepository,
      BuildConfig.DEFAULT,
      RouterConfig.DEFAULT,
      DataImportIssueSummary.empty(),
      emissionsDataModel,
      null,
      streetLimitationParameters
    );
    serializedObj.save(new FileDataSource(tempFile, FileType.GRAPH));
    SerializedGraphObject deserializedGraph = SerializedGraphObject.load(tempFile);
    Graph copiedGraph1 = deserializedGraph.graph;
    TransitModel copiedTransitModel1 = deserializedGraph.transitModel;
    // Index both graph - we do no know if the original is indexed, because it is cached and
    // might be indexed by other tests.

    originalTransitModel.index();
    originalGraph.index(originalTransitModel.getStopModel());

    copiedTransitModel1.index();
    copiedGraph1.index(copiedTransitModel1.getStopModel());

    assertNoDifferences(originalGraph, copiedGraph1);

    SerializedGraphObject deserializedGraph2 = SerializedGraphObject.load(tempFile);
    Graph copiedGraph2 = deserializedGraph2.graph;
    TransitModel copiedTransitModel2 = deserializedGraph2.transitModel;
    copiedTransitModel2.index();
    copiedGraph2.index(copiedTransitModel2.getStopModel());
    assertNoDifferences(copiedGraph1, copiedGraph2);
  }
}
