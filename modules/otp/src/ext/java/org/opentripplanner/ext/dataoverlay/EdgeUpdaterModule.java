package org.opentripplanner.ext.dataoverlay;

import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.dataoverlay.configuration.TimeUnit;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;

/**
 * This class allows updating the graph with the grid data from generic .nc file in accordance with
 * provided json configuration
 *
 * @author Simeon Platonov
 */
public class EdgeUpdaterModule implements GraphBuilderModule {

  private final GenericDataFile dataFile;
  private final TimeUnit timeFormat;
  private final DataOverlayParameterBindings parameterBindings;
  private final Graph graph;

  /**
   * Sets the generic grid data file
   */
  public EdgeUpdaterModule(
    Graph graph,
    GenericDataFile dataFile,
    TimeUnit timeFormat,
    DataOverlayParameterBindings parameterBindings
  ) {
    this.graph = graph;
    this.dataFile = dataFile;
    this.timeFormat = timeFormat;
    this.parameterBindings = parameterBindings;
  }

  @Override
  public void buildGraph() {
    GenericEdgeUpdater genericEdgeUpdater = new GenericEdgeUpdater(
      dataFile,
      timeFormat,
      graph.getStreetEdges()
    );
    genericEdgeUpdater.updateEdges();
    // The bindings are needed to build the request context when routing
    graph.dataOverlayParameterBindings = this.parameterBindings;
  }
}
