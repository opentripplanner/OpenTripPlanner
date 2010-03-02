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

package org.opentripplanner.graph_builder.impl.ned;

import java.io.File;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.Coverage;
import org.opentripplanner.graph_builder.services.ned.NEDGridCoverageFactory;
import org.opentripplanner.routing.core.Graph;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Automatically downloads and caches NED to cover every vertex of a graph.
 */
public class NEDGridCoverageFactoryImpl implements NEDGridCoverageFactory {

    private Graph graph;

    UnifiedGridCoverage coverage = null;

    private File cacheDirectory;

    /**
     * Set the graph that will be used to determine the extent of the NED.
     * @param graph
     */
    @Autowired
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * Set the directory where NED will be cached.
     * @param cacheDirectory
     */
    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public Coverage getGridCoverage() {
        if (coverage == null) {
            NEDDownloader downloader = new NEDDownloader();
            downloader.setGraph(graph);
            downloader.setCacheDirectory(cacheDirectory);
            List<File> paths = downloader.downloadNED();
            for (File path : paths) {
                GeotiffGridCoverageFactoryImpl factory = new GeotiffGridCoverageFactoryImpl();
                factory.setPath(path);
                GridCoverage2D regionCoverage = factory.getGridCoverage();
                if (coverage == null) {
                    coverage = new UnifiedGridCoverage("unified", regionCoverage);
                } else {
                    coverage.add(regionCoverage);
                }
            }
        }
        return coverage;
    }
}
