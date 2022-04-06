package org.opentripplanner.graph_builder.services.ned;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.Coverage;
import org.opentripplanner.routing.graph.Graph;

/**
 * Factory interface specifying the ability to generate GeoTools {@link GridCoverage2D} objects
 * representing National Elevation Dataset (NED) raster data.
 *
 * @author demory
 */

public interface ElevationGridCoverageFactory {
  /** Creates a new coverage instance from files already fetched */
  Coverage getGridCoverage();

  void checkInputs();

  /**
   * Sets the graph of the factory and initiates the fetching of data that is not present in the
   * cache
   */
  void fetchData(Graph graph);
}
