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

import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListModel;
import org.opentripplanner.util.TransitStopConnToWantedEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mabu
 */
public class StationsListModel extends DefaultListModel<TransitStopConnToWantedEdge>{
    Map<String,Integer> transitStopToIndex;
    private static final Logger LOG = LoggerFactory.getLogger(StationsListModel.class);


    public StationsListModel() {
        transitStopToIndex = new HashMap<>();
    }

    @Override
    public void addElement(TransitStopConnToWantedEdge element) {
        String stop_id = element.getStopID();
        Integer on_index = transitStopToIndex.get(stop_id);
        if (on_index == null) {
            LOG.info("Adding {}", element);
            super.addElement(element); 
            //saves which stop id is in which index
            transitStopToIndex.put(stop_id, getSize()-1);
        } else {
            LOG.info("Replacing {} with {} on {}", get(on_index), element, on_index);
            super.set(on_index, element);
        }
    }

    @Override
    public void clear() {
        transitStopToIndex.clear();
        super.clear(); 
    }    
}
