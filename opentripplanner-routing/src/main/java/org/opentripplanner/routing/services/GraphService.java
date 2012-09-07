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

import java.util.Collection;

import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service classes that need access to core graph objects like {@link Graph},
 * {@link ContractionHierarchySet} and {@link CalendarService} should access
 * them through this service interface. Instead of injecting a {@link Autowired}
 * dependency on {@link Graph}, instead inject an instace of
 * {@link GraphService} and use the {@link #getGraph()} method to access the
 * graph as neeeed.
 * 
 * Why the level of indirection? The service interface allows use to more easily
 * decouple the deserialization, loading, and management of the underlying graph
 * objects from the classes that need access to the objects. This indirection
 * allows us to dynamically swap in a new graph if underlying data changes, for
 * example.
 * 
 * @author bdferris
 * 
 */
public interface GraphService {

    /** specify whether additional debug information is loaded from the serialized graphs */
    public void setLoadLevel(LoadLevel level);
    
    /** Refresh all known graphs. This will usually involve reloading the graph from a file. */
    public void refreshGraphs();

    /** @return the current default graph object */
    public Graph getGraph();

    /** @return the default graph object for the given router ID */
    public Graph getGraph(String routerId);

    public Collection<String> getGraphIds();

    /** Forces loading of all known graphs */
    void loadAllGraphs();

}
