package org.opentripplanner.routing.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;


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