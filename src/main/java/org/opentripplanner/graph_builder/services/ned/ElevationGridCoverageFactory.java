/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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