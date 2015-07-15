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

import org.opentripplanner.routing.graph.Graph;

import java.io.File;
import java.util.List;

/**
 * A source of NED tiles for NEDGridCoverageFactoryImpl -- maybe the USGS streaming
 * server, maybe one-degree tiles, maybe something else.
 * @author novalis
 */
public interface NEDTileSource {

    public abstract void setGraph(Graph graph);

    /**
     * The cache directory stores NED tiles.  It is crucial that this be somewhere permanent
     * with plenty of disk space.  Don't use /tmp -- the downloading process takes a long time
     * and you don't want to repeat it if at all possible.
     * @param cacheDirectory
     */
    public abstract void setCacheDirectory(File cacheDirectory);

    /**
     * Download all the NED tiles into the cache.
     */
    public abstract List<File> getNEDTiles();

}