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

/**
 * Service classes interested in being notified of graph refresh events should implement this class.
 * Implementations of {@link GraphService} will call {@link #handleGraphRefresh(GraphService)} when
 * a graph is refreshed through a call to {@link GraphService#refreshGraph()} or when the graph is
 * initial loaded.
 * 
 * @author bdferris
 * 
 */
public interface GraphRefreshListener {
    public void handleGraphRefresh(GraphService graphService);
}
