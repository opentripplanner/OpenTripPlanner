package org.opentripplanner.graph_builder.services.ned;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.Coverage;
import org.opentripplanner.routing.graph.Graph;

/**
 * Factory interface specifying the ability to generate GeoTools {@link GridCoverage2D} objects 
 * representing National Elevation Dataset (NED) raster data. 
 * 
 * @author demory
 *
 */

public interface ElevationGridCoverageFactory {
    public Coverage getGridCoverage();

    public void checkInputs();

    public void setGraph(Graph graph);
}