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

package org.opentripplanner.updater;

import org.opentripplanner.routing.graph.Graph;

/**
 * The graph should only be modified by a runnable implementing this interface, executed by the
 * GraphUpdaterManager.
 * A few notes:
 * - Don't spend more time in this runnable than necessary, it might block other graph writer runnables.
 * - Be aware that while only one graph writer runnable is running to write to the graph, several
 *   request-threads might be reading the graph.
 * - Be sure that the request-threads always see a consistent view of the graph while planning.
 * 
 * @see GraphUpdaterManager.execute
 */
public interface GraphWriterRunnable {

    /**
     * This function is executed to modify the graph.
     */
    public void run(Graph graph);
}
