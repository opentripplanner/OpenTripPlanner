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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.loader.NetworkLinker;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Envelope;

/**
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} plugin that links up the stops of a transit network to a street network.
 * Should be called after both the transit network and street network are loaded.
 */
public class TransitToStreetNetworkModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(TransitToStreetNetworkModule.class);

    public List<String> provides() {
        return Arrays.asList("street to transit", "linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets"); // why not "transit" ?
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Linking transit stops to streets...");
        
        double radius = 0.01;

    	graph.index(new DefaultStreetVertexIndexFactory());
        
        for (TransitStop tstop : Iterables.filter(graph.getVertices(), TransitStop.class)) {
        	Envelope envelope = new Envelope(tstop.getCoordinate());
        	envelope.expandBy(radius);
            Collection<Vertex> nearby = graph.streetIndex.getVerticesForEnvelope(envelope);
            
            double radiusErsatz = radius * radius;
            
            double bestErsatz = Double.POSITIVE_INFINITY;
            Vertex best = null;
            
            for (OsmVertex v : Iterables.filter(nearby, OsmVertex.class)) {
            	double dx = v.getLat() - tstop.getLat();
            	double dy = v.getLon() - tstop.getLon();
            	
            	double ersatz = dx * dx + dy * dy;
            	
            	// make the index deterministic
            	if (ersatz > radiusErsatz)
            		continue;
            	
            	if (ersatz < bestErsatz) {
            		best = v;
            		bestErsatz = ersatz;
            	}
            }
            
            if (best != null) {
            	new FreeEdge(best, tstop);
            	new FreeEdge(tstop, best);
            }
        }
    }

    @Override
    public void checkInputs() {
        //no inputs
    }
}
