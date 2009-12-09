/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

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
package org.opentripplanner.routing.edgetype.loader;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.vertextypes.TransitStop;

public class GTFSPatternHopLoader {

    private Graph _graph;

    private GtfsRelationalDao _dao;

    private GtfsContext _context;

    public GTFSPatternHopLoader(Graph graph, GtfsContext context) {
        _graph = graph;
        _context = context;
        _dao = context.getDao();
    }

    public void load(boolean verbose) throws Exception {

        // Load stops
        for (Stop stop : _dao.getAllStops()) {
            Vertex vertex = _graph.addVertex(new GenericVertex(id(stop.getId()), stop.getLon(),
                    stop.getLat(), stop.getName(), TransitStop.class));
        }

        // Load hops
        if (verbose) {
            System.out.println("Loading hops");
        }
        GTFSPatternHopFactory hf = new GTFSPatternHopFactory(_context);
        hf.run(_graph);
    }

    private String id(AgencyAndId id) {
        return id.getAgencyId() + "_" + id.getId();
    }

    public void load() throws Exception {
        load(false);
    }

}
