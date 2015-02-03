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

package org.opentripplanner.graph_builder.module;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.openstreetmap.services.RegionsSource;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * A rectangular region bounding a set of transit stops
 *
 */
public class TransitStopsRegionsSourceImpl implements RegionsSource {
    private static Logger LOG = LoggerFactory.getLogger(TransitStopsRegionsSourceImpl.class);

    private static final double METERS_PER_DEGREE_LAT = 111111;
    private double distance = 2000;

    // FIXME replace autowiring
    GraphBuilder task;
    
    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public Iterable<Envelope> getRegions() {

    	List<Envelope> regions = new ArrayList<Envelope>();

        for (Vertex gv : task.getGraph().getVertices()) {
            if (gv instanceof TransitStop) {
                Coordinate c = gv.getCoordinate();
                Envelope env = new Envelope(c);
                double meters_per_degree_lon_here =  
                    METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(c.y));
                env.expandBy(distance / meters_per_degree_lon_here,  
                        distance / METERS_PER_DEGREE_LAT);
                regions.add(env);
            }
        }

        LOG.debug("Total regions: " + regions.size());
        
        return regions;
    }
}
