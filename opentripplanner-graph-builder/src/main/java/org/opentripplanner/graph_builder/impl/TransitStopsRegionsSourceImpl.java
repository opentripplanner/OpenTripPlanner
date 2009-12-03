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
package org.opentripplanner.graph_builder.impl;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.graph_builder.services.RegionsSource;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.vertextypes.TransitStop;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Envelope;

public class TransitStopsRegionsSourceImpl implements RegionsSource {

    private Graph _graph;

    @Autowired
    public void setGraph(Graph graph) {
        _graph = graph;
    }

    @Override
    public Iterable<Envelope> getRegions() {

        List<Envelope> regions = new ArrayList<Envelope>();

        for (Vertex vertex : _graph.getVertices()) {
            if (vertex.getType()  == TransitStop.class) { 
                Envelope env = new Envelope(vertex.getCoordinate());
                // TODO - Would be nice to express this in meters
                env.expandBy(0.02);
                regions.add(env);
            }
        }

        return regions;
    }
}
