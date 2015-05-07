package org.opentripplanner.updater.street_notes;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.notes.DynamicStreetNotesSource;
import org.opentripplanner.routing.services.notes.MatcherAndAlert;
import org.opentripplanner.routing.services.notes.NoteMatcher;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * A graph updater that reads a WFS-interface and updates a DynamicStreetNotesSource.
 * Useful when reading geodata from legacy/external sources, which are not based on OSM
 * and where data has to be matched to the street network.
 *
 * Classes that extend this class should provide getNote which parses the WFS features
 * into notes. Also the implementing classes should be added to the GraphUpdaterConfigurator
 *
 * @see WinkkiPollingGraphUpdater
 *
 * @author hannesj
 */
public abstract class WFSNotePollingGraphUpdater extends PollingGraphUpdater {
    protected Graph graph;

    private GraphUpdaterManager updaterManager;

    private SetMultimap<Edge, MatcherAndAlert> notesForEdge;

    /**
     * Set of unique matchers, kept during building phase, used for interning (lots of note/matchers
     * are identical).
     */
    private Map<T2<NoteMatcher, Alert>, MatcherAndAlert> uniqueMatchers;

    private URL url;
    private String featureType;
    private Query query;

    private FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
    private DynamicStreetNotesSource notesSource = new DynamicStreetNotesSource();

    // How much should the geometries be padded with in order to be sure they intersect with graph edges
    private static final double SEARCH_RADIUS_M = 1;
    private static final double SEARCH_RADIUS_DEG = SphericalDistanceLibrary.metersToDegrees(SEARCH_RADIUS_M);

    // Set the matcher type for the notes, can be overridden in extending classes
    private static final NoteMatcher NOTE_MATCHER = StreetNotesService.ALWAYS_MATCHER;

    private static Logger LOG = LoggerFactory.getLogger(WFSNotePollingGraphUpdater.class);

    /**
     * Here the updater can be configured using the properties in the file 'Graph.properties'.
     * The property frequencySec is already read and used by the abstract base class.
     */
    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        url = new URL(config.path("url").asText());
        featureType = config.path("featureType").asText();
        this.graph = graph;
        LOG.info("Configured WFS polling updater: frequencySec={}, url={} and featureType={}",
                frequencySec, url.toString(), featureType);
    }

    /**
     * Here the updater gets to know its parent manager to execute GraphWriterRunnables.
     */
    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    /**
     * Setup the WFS data source and add the DynamicStreetNotesSource to the graph
     */
    @Override
    public void setup() throws IOException, FactoryException {
        LOG.info("Setup WFS polling updater");
        HashMap<String, Object> connectionParameters = new HashMap<>();
        connectionParameters.put(WFSDataStoreFactory.URL.key, url);
        WFSDataStore data = (new WFSDataStoreFactory()).createDataStore(connectionParameters);
        query = new Query(featureType); // Read only single feature type from the source
        query.setCoordinateSystem(CRS.decode("EPSG:4326", true)); // Get coordinates in WGS-84
        featureSource = data.getFeatureSource(featureType);

        graph.streetNotesService.addNotesSource(notesSource);
    }

    @Override
    public void teardown() {
        LOG.info("Teardown WFS polling updater");
    }

    /**
     * The function is run periodically by the update manager.
     * The extending class should provide the getNote method. It is not implemented here
     * as the requirements for different updaters can be vastly different dependent on the data source.
     */
    @Override
    protected void runPolling() throws IOException{
        LOG.info("Run WFS polling updater with hashcode: {}", this.hashCode());

        notesForEdge = HashMultimap.create();
        uniqueMatchers = new HashMap<>();

        FeatureIterator<SimpleFeature> features = featureSource.getFeatures(query).features();

        while ( features.hasNext()){
            SimpleFeature feature = features.next();
            if (feature.getDefaultGeometry() == null) continue;

            Alert alert = getNote(feature);
            if (alert == null) continue;

            Geometry geom = (Geometry) feature.getDefaultGeometry();
            Geometry searchArea = geom.buffer(SEARCH_RADIUS_DEG);
            Collection<Edge> edges = graph.streetIndex.getEdgesForEnvelope(searchArea.getEnvelopeInternal());
            for(Edge edge: edges){
                if (edge instanceof StreetEdge && !searchArea.disjoint(edge.getGeometry())) {
                    addNote(edge, alert, NOTE_MATCHER);
                }
            }
        }
        updaterManager.execute(new WFSGraphWriter());
    }

    /**
     * Parses a SimpleFeature and returns an Alert if the feature should create one.
     * The alert should be based on the fields specific for the specific WFS feed.
     */
    protected abstract Alert getNote(SimpleFeature feature);

    /**
     * Changes the note source to use the newly generated notes
     */
    private class WFSGraphWriter implements GraphWriterRunnable {
        public void run(Graph graph) {
            notesSource.setNotes(notesForEdge);
        }
    }

    /**
     * Methods for writing into notesForEdge
     * TODO: Should these be extracted into somewhere?
     */
    private void addNote(Edge edge, Alert note, NoteMatcher matcher) {
        if (LOG.isDebugEnabled())
            LOG.debug("Adding note {} to {} with matcher {}", note, edge, matcher);
        notesForEdge.put(edge, buildMatcherAndAlert(matcher, note));
    }

    /**
     * Create a MatcherAndAlert, interning it if the note and matcher pair is already created. Note:
     * we use the default Object.equals() for matchers, as they are mostly already singleton
     * instances.
     */
    private MatcherAndAlert buildMatcherAndAlert(NoteMatcher noteMatcher, Alert note) {
        T2<NoteMatcher, Alert> key = new T2<>(noteMatcher, note);
        MatcherAndAlert interned = uniqueMatchers.get(key);
        if (interned != null) {
            return interned;
        }
        MatcherAndAlert ret = new MatcherAndAlert(noteMatcher, note);
        uniqueMatchers.put(key, ret);
        return ret;
    }

}
