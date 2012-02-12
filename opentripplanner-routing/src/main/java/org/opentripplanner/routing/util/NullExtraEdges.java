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

package org.opentripplanner.routing.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;


/** 
 * 
 * NullExtraEdges is used to speed up checks for extra edges in the (common) case 
 * where there are none. Extra edges come from StreetLocationFinder, where 
 * they represent the edges between a location on a street segment and the 
 * corners at the ends of that segment. 
 */
public class NullExtraEdges implements Map<Vertex, ArrayList<Edge>> {

    @Override
    public void clear() {
    }

    @Override
    public boolean containsKey(Object arg0) {
        return false;
    }

    @Override
    public boolean containsValue(Object arg0) {
        return false;
    }

    @Override
    public Set<java.util.Map.Entry<Vertex, ArrayList<Edge>>> entrySet() {
        return null;
    }

    @Override
    public ArrayList<Edge> get(Object arg0) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Set<Vertex> keySet() {
        return null;
    }

    @Override
    public ArrayList<Edge> put(Vertex arg0, ArrayList<Edge> arg1) {
        return null;
    }

    @Override
    public void putAll(Map<? extends Vertex, ? extends ArrayList<Edge>> arg0) {
    }

    @Override
    public ArrayList<Edge> remove(Object arg0) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Collection<ArrayList<Edge>> values() {
        return null;
    }
}