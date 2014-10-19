/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.opentripplanner.visualizer;

import java.util.logging.Level;
import javax.swing.DefaultListModel;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.StreetType;
import org.opentripplanner.util.TransitStopConnToWantedEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mabu
 */
public class CurStationConModel extends DefaultListModel<TranstiStopOrStreetEdge>{
    Integer vertex_index = null;
    Integer edge_index = null;
    private static final Logger LOG = LoggerFactory.getLogger(CurStationConModel.class);

    @Override
    public void clear() {
        vertex_index = null;
        edge_index = null;
        super.clear(); 
    }
  
    public void addTransitStop(TransitStop element) throws Exception {
        if (getSize() == 2) {
            throw new Exception("Current station can see only 2 elements at the same time!");
        }
        if (vertex_index == null) {    
            vertex_index = getSize();
            super.addElement(new TranstiStopOrStreetEdge(element)); 
            LOG.info("Added vertex with index: {}", vertex_index);
        } else {
            throw new Exception("Current station can have only one TransitStop!");
        }
    }
    
    public void addStreetEdge(StreetEdge element) throws Exception {
        if (edge_index == null) {
            edge_index = getSize();
            super.addElement(new TranstiStopOrStreetEdge(element));
            LOG.info("Added edge with index: {}", edge_index);
        } else {
            super.set(edge_index, new TranstiStopOrStreetEdge(element));
        }
    }
    
    public boolean hasVertexAndStreet() {
        boolean retval = (vertex_index != null) && (edge_index != null);
        LOG.info("hasVertexAndStreet {} bool: {}", getSize(), retval);
        return retval;
    }
    
    public TransitStopConnToWantedEdge getTransitStopConWantedEdge() {
        StreetEdge se = get(edge_index).wantedPath;
        StreetType st = StreetType.NORMAL;
        if (se.getName().equals("service road")) {
            st = StreetType.SERVICE;
        } else if ((se.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)
                    || (se.getPermission().allows(StreetTraversalPermission.BICYCLE)))
                    && !(se.getPermission().allows(StreetTraversalPermission.CAR))) {
            st = StreetType.WALK_BIKE;
        }
        return new TransitStopConnToWantedEdge(get(vertex_index).transitStop, se, st);
    }

    @Override
    public void addElement(TranstiStopOrStreetEdge element) {
        //Do nothing. All inserts need to be with addTransitElement
    }

    void addEdit(TransitStopConnToWantedEdge selectedValue) {
        try {
            addTransitStop(selectedValue.getTransitStop());
            addStreetEdge(selectedValue.getStreetEdge());
        } catch (Exception ex) {
            LOG.error("Error adding wanted Edit edges.", ex);
        }
    }

    TransitStop getVertex() {
        return get(vertex_index).transitStop;
    }
    
    
    
    
    
}
