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

package org.opentripplanner.decoration;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.routing.graph.Graph;

/**
 * Shutdown service that client can use if they want to register hooks when a graph is being
 * shutdown (evinced).
 */
public class ShutdownGraphService {

    public interface GraphShutdownHook {
        public abstract void onShutdown(Graph graph);
    }

    private List<GraphShutdownHook> graphShutdownHooks = new ArrayList<GraphShutdownHook>();

    public void addGraphShutdownHook(GraphShutdownHook shutdownHook) {
        graphShutdownHooks.add(shutdownHook);
    }

    protected void shutdown(Graph graph) {
        for (GraphShutdownHook shutdownHook : graphShutdownHooks) {
            shutdownHook.onShutdown(graph);
        }
    }

}
