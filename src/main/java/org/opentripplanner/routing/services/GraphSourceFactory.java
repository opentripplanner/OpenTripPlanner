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

package org.opentripplanner.routing.services;

import java.io.InputStream;

/**
 * A factory of GraphSource.
 * 
 */
public interface GraphSourceFactory {

    public GraphSource createGraphSource(String routerId);

    /**
     * Save the graph data, but don't load it in memory. The file location is based on the router
     * id. If the graph already exists, the graph will be overwritten. The relationship between
     * router IDs and paths in the filesystem is determined by the graphService implementation.
     * 
     * @param routerId the routerId of the graph
     * @param is graph data as input stream
     * @return
     */
    public boolean save(String routerId, InputStream is);
}
