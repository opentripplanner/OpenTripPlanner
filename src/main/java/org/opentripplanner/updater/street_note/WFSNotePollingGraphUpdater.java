package org.opentripplanner.updater.street_note;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.notes.DynamicStreetNotesSource;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.model.note.StreetNoteAndMatcher;
import org.opentripplanner.street.model.note.StreetNoteMatcher;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A graph updater that reads a WFS-interface and updates a DynamicStreetNotesSource. Useful when
 * reading geodata from legacy/external sources, which are not based on OSM and where data has to be
 * matched to the street network.
 * <p>
 * Classes that extend this class should provide getNote which parses the WFS features into notes.
 * Also the implementing classes should be added to the GraphUpdaterConfigurator
 *
 * @see WinkkiPollingGraphUpdater
 */
public abstract class WFSNotePollingGraphUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(WFSNotePollingGraphUpdater.class);

  /**
   * How much should the geometries be padded with in order to be sure they intersect with
   * graph edges
   */
  private static final double SEARCH_RADIUS_M = 1;
  private static final double SEARCH_RADIUS_DEG = SphericalDistanceLibrary.metersToDegrees(
    SEARCH_RADIUS_M
  );

  /** Set the matcher type for the notes */
  private static final StreetNoteMatcher NOTE_MATCHER = StreetNotesService.ALWAYS_MATCHER;

  private final DynamicStreetNotesSource notesSource = new DynamicStreetNotesSource();
  private final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
  private final Query query;
  private final Graph graph;
  private WriteToGraphCallback saveResultOnGraph;
  private SetMultimap<Edge, StreetNoteAndMatcher> notesForEdge;

  /**
   * Set of unique matchers, kept during building phase, used for interning (lots of note/matchers
   * are identical).
   */
  private Map<StreetNoteAndMatcher, StreetNoteAndMatcher> uniqueMatchers;

  /**
   * The property 'frequency' is already read and used by the abstract base class.
   */
  public WFSNotePollingGraphUpdater(WFSNotePollingGraphUpdaterParameters config, Graph graph) {
    super(config);
    try {
      LOG.info("Setup WFS polling updater");
      URL url = new URL(config.getUrl());
      String featureType = config.getFeatureType();

      this.graph = graph;

      HashMap<String, Object> connectionParameters = new HashMap<>();
      connectionParameters.put(WFSDataStoreFactory.URL.key, url);
      WFSDataStore data = (new WFSDataStoreFactory()).createDataStore(connectionParameters);

      this.query = new Query(featureType); // Read only single feature type from the source
      this.query.setCoordinateSystem(CRS.decode("EPSG:4326", true)); // Get coordinates in WGS-84
      this.featureSource = data.getFeatureSource(featureType);
      graph.streetNotesService.addNotesSource(notesSource);

      LOG.info(
        "Configured WFS polling updater: frequency={}, url={} and featureType={}",
        pollingPeriod(),
        url.toString(),
        featureType
      );
    } catch (FactoryException | IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Here the updater gets to know its parent manager to execute GraphWriterRunnables.
   */
  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  /**
   * The function is run periodically by the update manager. The extending class should provide the
   * getNote method. It is not implemented here as the requirements for different updaters can be
   * vastly different dependent on the data source.
   */
  @Override
  protected void runPolling() throws IOException {
    LOG.info("Run WFS polling updater with hashcode: {}", this.hashCode());

    notesForEdge = HashMultimap.create();
    uniqueMatchers = new HashMap<>();

    FeatureIterator<SimpleFeature> features = featureSource.getFeatures(query).features();

    while (features.hasNext()) {
      SimpleFeature feature = features.next();
      if (feature.getDefaultGeometry() == null) continue;

      StreetNote streetNote = getNote(feature);
      if (streetNote == null) continue;

      Geometry geom = (Geometry) feature.getDefaultGeometry();
      Geometry searchArea = geom.buffer(SEARCH_RADIUS_DEG);
      Collection<Edge> edges = graph
        .getStreetIndex()
        .getEdgesForEnvelope(searchArea.getEnvelopeInternal());
      for (Edge edge : edges) {
        if (edge instanceof StreetEdge && !searchArea.disjoint(edge.getGeometry())) {
          addNote(edge, streetNote, NOTE_MATCHER);
        }
      }
    }
    saveResultOnGraph.execute(new WFSGraphWriter());
  }

  /**
   * Parses a SimpleFeature and returns an StreetNote if the feature should create one. The street
   * note should be based on the fields specific for the specific WFS feed.
   */
  protected abstract StreetNote getNote(SimpleFeature feature);

  /**
   * Methods for writing into notesForEdge
   * TODO: Should these be extracted into somewhere?
   */
  private void addNote(Edge edge, StreetNote note, StreetNoteMatcher matcher) {
    if (LOG.isDebugEnabled()) LOG.debug(
      "Adding note {} to {} with matcher {}",
      note,
      edge,
      matcher
    );
    notesForEdge.put(edge, buildMatcherAndStreetNote(matcher, note));
  }

  /**
   * Create a MatcherAndStreetNote, interning it if the note and matcher pair is already created.
   * Note: we use the default Object.equals() for matchers, as they are mostly already singleton
   * instances.
   */
  private StreetNoteAndMatcher buildMatcherAndStreetNote(
    StreetNoteMatcher noteMatcher,
    StreetNote note
  ) {
    var candidate = new StreetNoteAndMatcher(note, noteMatcher);
    var interned = uniqueMatchers.putIfAbsent(candidate, candidate);
    return interned == null ? candidate : interned;
  }

  /**
   * Changes the note source to use the newly generated notes
   */
  private class WFSGraphWriter implements GraphWriterRunnable {

    public void run(Graph graph, TransitModel transitModel) {
      notesSource.setNotes(notesForEdge);
    }
  }
}
