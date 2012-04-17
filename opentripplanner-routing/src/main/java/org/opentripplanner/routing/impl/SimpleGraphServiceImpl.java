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

package org.opentripplanner.routing.impl;

import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;

/**
 * This simple implementation of {@link GraphService} is mostly useful for testing
 * 
 * @see GraphServiceImpl
 * @see GraphService
 */
public class SimpleGraphServiceImpl implements GraphService {

    private Graph graph;

    private CalendarService calendarService;

    // TODO fix
    
    public SimpleGraphServiceImpl(Graph graph) {
        this.graph = graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /****
     * {@link GraphService} Interface
     ****/

    @Override
    public void refreshGraphs() {

    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public void setLoadLevel(LoadLevel level) {

    }

    @Override
    public Graph getGraph(String routerId) {
        // TODO Auto-generated method stub
        return null;
    }
}
