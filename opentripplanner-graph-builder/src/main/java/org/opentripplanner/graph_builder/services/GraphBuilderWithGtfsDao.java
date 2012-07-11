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

package org.opentripplanner.graph_builder.services;

import java.util.Collection;
import java.util.List;

import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.routing.graph.Graph;

/**
 * This is a special type of graph builder that needs access to the GTFS data
 * directly, rather than through the graph.  It is run at the end of GtfsGraphBuilderImpl
 * @author novalis
 *
 */
public interface GraphBuilderWithGtfsDao {
	public void setDao(GtfsRelationalDao dao);
	public void buildGraph(Graph graph);
        public List<String> provides();
        public List<String> getPrerequisites();
}
