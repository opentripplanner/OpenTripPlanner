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

import javax.media.jai.InterpolationBilinear;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.opengis.coverage.Coverage;
import org.opentripplanner.graph_builder.services.ned.NEDGridCoverageFactory;
import org.opentripplanner.graph_builder.services.ned.NEDTileSource;
import org.opentripplanner.routing.services.GraphService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A coverage factory that works off of the NED caches from {@link NEDDownloader}.
 */
public class NEDGridCoverageFactoryImpl implements NEDGridCoverageFactory {

    private GraphService graphService;

    UnifiedGridCoverage coverage = null;

    private File cacheDirectory;

    private NEDTileSource tileSource = new NEDDownloader();

    /**
     * Set the graph that will be used to determine the extent of the NED.
     *
     * @param graphService
     */
    @Autowired
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * Set the directory where NED will be cached.
     *
     * @param cacheDirectory
     */
    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    @Autowired(required=false)
    public void setTileSource(NEDTileSource source) {
        this.tileSource = source;
    }

    public Coverage getGridCoverage() {
        if (coverage == null) {
            tileSource.setGraph(graphService.getGraph());
            tileSource.setCacheDirectory(cacheDirectory);
            List<File> paths = tileSource.getNEDTiles();
            for (File path : paths) {
                GeotiffGridCoverageFactoryImpl factory = new GeotiffGridCoverageFactoryImpl();
                factory.setPath(path);
                GridCoverage2D regionCoverage = Interpolator2D.create(factory.getGridCoverage(),
                        new InterpolationBilinear());
                if (coverage == null) {
                    coverage = new UnifiedGridCoverage("unified", regionCoverage);
                } else {
                    coverage.add(regionCoverage);
                }
            }
        }
        return coverage;
    }

    @Override
    public void checkInputs() {
        if (!cacheDirectory.canWrite()) {
            throw new RuntimeException("Can't write to NED cache: " + cacheDirectory);
        }
    }
}