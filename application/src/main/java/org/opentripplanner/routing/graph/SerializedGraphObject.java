package org.opentripplanner.routing.graph;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.framework.geometry.CompactElevationProfile;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.model.projectinfo.GraphFileHeader;
import org.opentripplanner.model.projectinfo.OtpProjectInfo;
import org.opentripplanner.routing.graph.kryosupport.KryoBuilder;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.utils.lang.OtpNumberFormat;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the class that get serialized/deserialized into/from the file <em>graph.obj</em>.
 * <p>
 * The Graph object does not contain a collection of edges. The set of edges is generated on demand
 * from the vertices. However, when serializing, we intentionally do not serialize the vertices'
 * edge lists to prevent excessive recursion. So we need to save the edges along with the graph. We
 * used to make two serialization calls, one for the graph and one for the edges. But we need the
 * serializer to know that vertices referenced by the edges are the same vertices stored in the
 * graph itself. The easiest way to do this is to make only one serialization call, serializing a
 * single object that contains both the graph and the edge collection.
 */
public class SerializedGraphObject implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(SerializedGraphObject.class);

  public final Graph graph;

  @Nullable
  public final OsmInfoGraphBuildRepository osmInfoGraphBuildRepository;

  public final TimetableRepository timetableRepository;
  public final WorldEnvelopeRepository worldEnvelopeRepository;
  private final Collection<Edge> edges;

  /**
   * The config JSON used to build this graph. Allows checking whether the configuration has
   * changed.
   */
  public final BuildConfig buildConfig;

  /** Embed a router configuration inside the graph, for starting up with a single file. */
  public final RouterConfig routerConfig;

  /**
   * All submodes are cached in a static collection inside SubMode,
   * hence we need to serialize that as well
   */
  private final List<SubMode> allTransitSubModes;

  public final DataImportIssueSummary issueSummary;
  public final StopConsolidationRepository stopConsolidationRepository;
  private final int routingTripPatternCounter;
  public final EmissionsDataModel emissionsDataModel;
  public final StreetLimitationParameters streetLimitationParameters;
  public final VehicleParkingRepository parkingRepository;

  public SerializedGraphObject(
    Graph graph,
    @Nullable OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    TimetableRepository timetableRepository,
    WorldEnvelopeRepository worldEnvelopeRepository,
    VehicleParkingRepository parkingRepository,
    BuildConfig buildConfig,
    RouterConfig routerConfig,
    DataImportIssueSummary issueSummary,
    EmissionsDataModel emissionsDataModel,
    StopConsolidationRepository stopConsolidationRepository,
    StreetLimitationParameters streetLimitationParameters
  ) {
    this.graph = graph;
    this.edges = graph.getEdges();
    this.osmInfoGraphBuildRepository = osmInfoGraphBuildRepository;
    this.timetableRepository = timetableRepository;
    this.worldEnvelopeRepository = worldEnvelopeRepository;
    this.parkingRepository = parkingRepository;
    this.buildConfig = buildConfig;
    this.routerConfig = routerConfig;
    this.issueSummary = issueSummary;
    this.emissionsDataModel = emissionsDataModel;
    this.allTransitSubModes = SubMode.listAllCachedSubModes();
    this.routingTripPatternCounter = RoutingTripPattern.indexCounter();
    this.stopConsolidationRepository = stopConsolidationRepository;
    this.streetLimitationParameters = streetLimitationParameters;
  }

  public static void verifyTheOutputGraphIsWritableIfDataSourceExist(DataSource graphOutput) {
    if (graphOutput != null) {
      // Abort building a graph if the file can not be saved
      if (graphOutput.exists()) {
        LOG.info(
          "Graph already exists and will be overwritten at the end of the " +
          "build process. Graph: {}",
          graphOutput.path()
        );
      }
      if (!graphOutput.isWritable()) {
        throw new RuntimeException("Cannot create or write to graph at: " + graphOutput.path());
      }
    }
  }

  public static SerializedGraphObject load(DataSource source) {
    return load(source.asInputStream(), source.path());
  }

  public static SerializedGraphObject load(File file) {
    try {
      return load(new FileInputStream(file), file.getAbsolutePath());
    } catch (FileNotFoundException e) {
      LOG.error("Graph file not found: " + file, e);
      throw new OtpAppException(e.getMessage());
    }
  }

  /**
   * After deserialization, the vertices will all have null outgoing and incoming edge lists because
   * those edge lists are marked transient, to prevent excessive recursion depth while serializing.
   * This method will reconstruct all those edge lists after deserialization.
   */
  public void reconstructEdgeLists() {
    for (Vertex v : graph.getVertices()) {
      v.initEdgeLists();
    }
    for (Edge e : edges) {
      Vertex fromVertex = e.getFromVertex();
      Vertex toVertex = e.getToVertex();
      fromVertex.addOutgoing(e);
      toVertex.addIncoming(e);
    }
  }

  /**
   * Save this object to the target it the target data source is not {@code null}.
   */
  public void save(@Nullable DataSource target) {
    if (target != null) {
      save(target.asOutputStream(), target.name(), target.size());
    } else {
      LOG.info("Not saving graph to disk, as requested.");
    }
  }

  /* private methods */

  private static SerializedGraphObject load(InputStream inputStream, String sourceDescription) {
    // TODO store version information, halt load if versions mismatch
    try (inputStream) {
      LOG.info("Reading graph from '{}'", sourceDescription);
      Input input = new Input(inputStream);

      validateGraphSerializationId(
        input.readBytes(GraphFileHeader.headerLength()),
        sourceDescription
      );

      Kryo kryo = KryoBuilder.create();
      SerializedGraphObject serObj = (SerializedGraphObject) kryo.readClassAndObject(input);
      SubMode.deserializeSubModeCache(serObj.allTransitSubModes);
      RoutingTripPattern.initIndexCounter(serObj.routingTripPatternCounter);
      CompactElevationProfile.setDistanceBetweenSamplesM(
        serObj.graph.getDistanceBetweenElevationSamples()
      );
      LOG.debug("Graph read.");
      serObj.reconstructEdgeLists();
      serObj.timetableRepository.getSiteRepository().reindexAfterDeserialization();
      serObj.timetableRepository.index();
      logSerializationCompleteStatus(serObj.graph, serObj.timetableRepository);
      return serObj;
    } catch (IOException e) {
      LOG.error("IO exception while loading graph: {}", e.getLocalizedMessage(), e);
      return null;
    } catch (KryoException ke) {
      if (ke.getCause() instanceof IOException) {
        LOG.error("IO exception while loading graph: {}", ke.getLocalizedMessage(), ke);
        return null;
      }
      LOG.warn(
        "Deserialization exception while loading graph: {}\n{}",
        sourceDescription,
        ke.getLocalizedMessage()
      );
      throw new OtpAppException(
        "Unable to load graph. The deserialization failed. Is the " +
        "loaded graph build with the same OTP version as you are using to load it? " +
        "Graph: " +
        sourceDescription
      );
    }
  }

  @SuppressWarnings("Convert2MethodRef")
  private static OutputStream wrapOutputStreamWithProgressTracker(
    OutputStream outputStream,
    long size
  ) {
    return ProgressTracker.track(
      "Save graph",
      500_000,
      size,
      outputStream,
      // Keep this to get correct logging info for class and line number
      msg -> LOG.info(msg)
    );
  }

  private static void validateGraphSerializationId(byte[] header, String sourceName) {
    var expFileHeader = projectInfo().graphFileHeaderInfo;
    var graphFileHeader = GraphFileHeader.parse(header);

    if (!expFileHeader.equals(graphFileHeader)) {
      if (!expFileHeader.equals(graphFileHeader)) {
        throw new OtpAppException(
          "The graph file is incompatible with this version of OTP. " +
          "The OTP serialization version id '%s' do not match the id " +
          "'%s' in '%s' file-header.",
          expFileHeader.otpSerializationVersionId(),
          graphFileHeader.otpSerializationVersionId(),
          sourceName
        );
      }
    }
  }

  private void save(OutputStream outputStream, String graphName, long size) {
    LOG.info("Writing graph {}  ...", graphName);
    outputStream = wrapOutputStreamWithProgressTracker(outputStream, size);
    Kryo kryo = KryoBuilder.create();
    Output output = new Output(outputStream);
    output.write(OtpProjectInfo.projectInfo().graphFileHeaderInfo.header());
    kryo.writeClassAndObject(output, this);
    output.close();
    LOG.info("Graph written: {}", graphName);
    // Summarize serialized classes and associated serializers to stdout:
    // ((InstanceCountingClassResolver) kryo.getClassResolver()).summarize();
  }

  private static void logSerializationCompleteStatus(
    Graph graph,
    TimetableRepository timetableRepository
  ) {
    var f = new OtpNumberFormat();
    var nStops = f.formatNumber(timetableRepository.getSiteRepository().stopIndexSize());
    var nTransfers = f.formatNumber(timetableRepository.getTransferService().listAll().size());
    var nPatterns = f.formatNumber(timetableRepository.getAllTripPatterns().size());
    var nVertices = f.formatNumber(graph.countVertices());
    var nEdges = f.formatNumber(graph.countEdges());

    LOG.info("Graph loaded.   |V|={} |E|={}", nVertices, nEdges);
    LOG.info(
      "Transit loaded. |Stops|={} |Patterns|={} |ConstrainedTransfers|={}",
      nStops,
      nPatterns,
      nTransfers
    );
  }
}
