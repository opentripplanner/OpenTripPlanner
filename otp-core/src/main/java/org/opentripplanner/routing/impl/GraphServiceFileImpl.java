/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.util.WeakCollectionCleaner;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.updater.PropertiesPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

/**
 * A class implementing loading graph from files or resources, but which does not load anything by
 * itself. It rely on owner instances to help it initialize/reload itself.
 * 
 * Note: Naming is not ideal, but this would have broke down the spring API widely used (namely, the
 * GraphServiceImpl class).
 * 
 * @see GraphServiceImpl
 * @see GraphServiceAutoDiscoverImpl
 */
public class GraphServiceFileImpl implements GraphService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceFileImpl.class);

    public static final String GRAPH_FILENAME = "Graph.obj";

    public static final String CONFIG_FILENAME = "Graph.properties";

    @Getter
    @Setter
    private String basePath = "/var/otp/graphs";

    private Map<String, Graph> graphs = new HashMap<String, Graph>();

    private Map<String, LoadLevel> levels = new HashMap<String, LoadLevel>();

    private LoadLevel loadLevel = LoadLevel.FULL;

    private GraphUpdaterConfigurator decorator = new GraphUpdaterConfigurator();

    @Setter
    private StreetVertexIndexFactory indexFactory = new DefaultStreetVertexIndexFactory();

    @Setter
    @Getter
    private String defaultRouterId = "";

    /**
     * Router IDs may contain alphanumeric characters, underscores, and dashes only. This prevents
     * any confusion caused by the presence of special characters that might have a meaning for the
     * filesystem.
     */
    public static final Pattern routerIdPattern = Pattern.compile("[\\p{Alnum}_-]*");

    public Graph getGraph() {
        return getGraph(null);
    }

    public Graph getGraph(String routerId) {
        if (routerId == null || routerId.isEmpty() || routerId.equalsIgnoreCase("default")) {
            routerId = defaultRouterId;
            LOG.debug("routerId not specified, set to default of '{}'", routerId);
        }
        synchronized (graphs) {
            if (!graphs.containsKey(routerId)) {
                LOG.error("no graph registered with the routerId '{}'", routerId);
                throw new GraphNotFoundException();
            } else {
                return graphs.get(routerId);
            }
        }
    }

    public void setLoadLevel(LoadLevel level) {
        if (level != loadLevel) {
            loadLevel = level;
            reloadGraphs(true);
        }
    }

    private boolean routerIdLegal(String routerId) {
        Matcher m = routerIdPattern.matcher(routerId);
        return m.matches();
    }

    private String createBaseFileName(String routerId) {
        StringBuilder sb = new StringBuilder(basePath);
        if (!(basePath.endsWith(File.separator))) {
            sb.append(File.separator);
        }
        if (routerId.length() > 0) {
            // there clearly must be a more elegant way to extend paths
            sb.append(routerId);
            sb.append(File.separator);
        }
        return sb.toString();
    }

    protected Graph loadGraph(String routerId) {
        if (!routerIdLegal(routerId)) {
            LOG.error(
                    "routerId '{}' contains characters other than alphanumeric, underscore, and dash.",
                    routerId);
            return null;
        }
        LOG.debug("loading serialized graph for routerId {}", routerId);

        String baseFileName = createBaseFileName(routerId);
        String graphFileName = baseFileName + GRAPH_FILENAME;
        String configFileName = baseFileName + CONFIG_FILENAME;

        LOG.debug("graph file for routerId '{}' is at {}", routerId, graphFileName);
        InputStream is = null;
        final String CLASSPATH_PREFIX = "classpath:/";
        if (graphFileName.startsWith(CLASSPATH_PREFIX)) {
            // look for graph on classpath
            String resourceName = graphFileName.substring(CLASSPATH_PREFIX.length());
            LOG.debug("loading graph on classpath at {}", resourceName);
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        } else {
            // look for graph in filesystem
            try {
                File graphFile = new File(graphFileName);
                is = new FileInputStream(graphFile);
            } catch (IOException ex) {
                is = null;
                LOG.warn("Error creating graph input stream", ex);
            }
        }
        if (is == null) {
            LOG.warn("Graph file not found or not openable for routerId '{}' under {}", routerId,
                    graphFileName);
            return null;
        }
        LOG.debug("graph input stream successfully opened.");
        LOG.info("Loading graph...");
        Graph graph = null;
        try {
            graph = Graph.load(new ObjectInputStream(is), loadLevel, indexFactory);
        } catch (Exception ex) {
            LOG.error("Exception while loading graph from {}.", graphFileName);
            ex.printStackTrace();
            return null;
        }
        // Decorate the graph. Even if a config file is not present
        // one could be bundled inside.
        try {
            is = null;
            if (configFileName.startsWith(CLASSPATH_PREFIX)) {
                // look for config on classpath
                String resourceName = configFileName.substring(CLASSPATH_PREFIX.length());
                LOG.debug("Trying to load config on classpath at {}", resourceName);
                is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(resourceName);
            } else {
                // look for config in filesystem
                LOG.debug("Trying to load config on file at {}", configFileName);
                File configFile = new File(configFileName);
                if (configFile.canRead()) {
                    LOG.info("Loading config from file {}", configFileName);
                    is = new FileInputStream(configFile);
                }
            }
            Preferences config = is == null ? null : new PropertiesPreferences(is);
            decorator.setupGraph(graph, config);
        } catch (IOException e) {
            LOG.error("Can't read config file", e);
        }
        return graph;
    }

    @Override
    public boolean reloadGraphs(boolean preEvict) {
        boolean allSucceeded = true;
        synchronized (graphs) {
            for (String routerId : this.getRouterIds()) {
                boolean success = registerGraph(routerId, preEvict);
                allSucceeded &= success;
            }
        }
        return allSucceeded;
    }

    @Override
    public Collection<String> getRouterIds() {
        return new ArrayList<String>(graphs.keySet());
    }

    @Override
    public boolean registerGraph(String routerId, boolean preEvict) {
        if (preEvict)
            evictGraph(routerId);
        LOG.info("registering routerId '{}'", routerId);
        Graph graph = this.loadGraph(routerId);
        if (graph != null) {
            synchronized (graphs) {
                if (!preEvict)
                    evictGraph(routerId);
                graphs.put(routerId, graph);
            }
            levels.put(routerId, loadLevel);
            return true;
        }
        LOG.info("routerId '{}' was not registered (graph was null).", routerId);
        return false;
    }

    @Override
    public boolean registerGraph(String routerId, Graph graph) {
        Graph existing = graphs.put(routerId, graph);
        return existing == null;
    }

    @Override
    public boolean evictGraph(String routerId) {
        LOG.debug("evicting graph {}", routerId);
        synchronized (graphs) {
            Graph existing = graphs.remove(routerId);
            if (existing != null) {
                decorator.shutdownGraph(existing);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public int evictAll() {
        int n;
        synchronized (graphs) {
            n = graphs.size();
            for (String routerId : graphs.keySet()) {
                evictGraph(routerId);
            }
        }
        return n;
    }

    @Override
    public boolean save(String routerId, InputStream is) {
        String baseFileName = createBaseFileName(routerId);
        String graphFileName = baseFileName + GRAPH_FILENAME;

        try {

            // Create directory if necessary
            File sourceFile = new File(graphFileName);
            File directory = new File(sourceFile.getParentFile().getPath());
            if (!directory.exists()) {
                directory.mkdir();
            }

            // Store the stream to disk, to be sure no data will be lost make a temporary backup
            // file of the original file.

            // Make backup file
            sourceFile = new File(graphFileName);
            File destFile = null;
            if (sourceFile.exists()) {
                destFile = new File(graphFileName + ".bak");
                if (destFile.exists()) {
                    destFile.delete();
                }
                sourceFile.renameTo(destFile);
            }

            // Store the stream
            FileOutputStream os = new FileOutputStream(graphFileName);
            ByteStreams.copy(is, os);

            // And delete the backup file
            sourceFile = new File(graphFileName + ".bak");
            if (sourceFile.exists()) {
                sourceFile.delete();
            }

        } catch (Exception ex) {
            LOG.error("Exception while storing graph to {}.", graphFileName);
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Hook to cleanup various stuff of some used libraries (org.geotools), which depend on the
     * external client to call them for cleaning-up.
     */
    public void cleanupWebapp() {
        LOG.info("Web application shutdown: cleaning various stuff");
        WeakCollectionCleaner.DEFAULT.exit();
        DeferredAuthorityFactory.exit();
    }
}
