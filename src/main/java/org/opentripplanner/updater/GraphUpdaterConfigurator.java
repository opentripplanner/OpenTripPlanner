package org.opentripplanner.updater;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.annotation.ComponentAnnotationConfigurator;
import org.opentripplanner.annotation.ServiceType;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.UpdaterConfig;
import org.opentripplanner.util.ConstructorDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up and starts all the graph updaters.
 * <p>
 * Updaters are instantiated based on the updater parameters contained in UpdaterConfig. Updaters
 * are then setup by providing the graph as a parameter. Finally, the updaters are added to the
 * GraphUpdaterManager.
 */
public abstract class GraphUpdaterConfigurator {

  private static Logger LOG = LoggerFactory.getLogger(GraphUpdaterConfigurator.class);

  public static void setupGraph(Graph graph, UpdaterConfig updaterConfig) {

    List<GraphUpdater> updaters = new ArrayList<>();

    updaters.addAll(createUpdatersFromConfig(updaterConfig));

    setupUpdaters(graph, updaters);
    GraphUpdaterManager updaterManager = new GraphUpdaterManager(graph, updaters);
    updaterManager.startUpdaters();

    // Stop the updater manager if it contains nothing
    if (updaterManager.size() == 0) {
      updaterManager.stop();
    }
    // Otherwise add it to the graph
    else {
      graph.updaterManager = updaterManager;
    }
  }

  /**
   * @return a list of GraphUpdaters created from the configuration
   */
  private static List<GraphUpdater> createUpdatersFromConfig(UpdaterConfig config) {
    List<GraphUpdater> updaters = new LinkedList<>();
    for (String type : config.getTypes()) {
      // For each sub-node, determine which kind of updater is being created.
      ConstructorDescriptor descriptor = ComponentAnnotationConfigurator.getInstance()
          .getConstructorDescriptor(type, ServiceType.GraphUpdater);
      config.getParameters(type).stream().map(para ->
          descriptor.newInstance(para)
      ).filter(Objects::nonNull).forEach(u -> updaters.add((GraphUpdater) u));
    }
    return updaters;
  }

  public static void shutdownGraph(Graph graph) {
    GraphUpdaterManager updaterManager = graph.updaterManager;
    if (updaterManager != null) {
      LOG.info("Stopping updater manager with " + updaterManager.size() + " updaters.");
      updaterManager.stop();
    }
  }

  public static void setupUpdaters(Graph graph, List<GraphUpdater> updaters) {
    for (GraphUpdater updater : updaters) {
      try {
        updater.setup(graph);
      } catch (Exception e) {
        LOG.warn("Failed to setup updater {}", updater.getName());
      }
    }
  }
}
