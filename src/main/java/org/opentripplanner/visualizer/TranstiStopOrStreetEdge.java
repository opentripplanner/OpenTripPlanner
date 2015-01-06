/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentripplanner.visualizer;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 *
 * @author mabu
 */
public class TranstiStopOrStreetEdge {
    TransitStop transitStop = null;
    StreetEdge wantedPath = null;

    public TranstiStopOrStreetEdge(TransitStop transitStop) {
        this.transitStop = transitStop;
    }

    public TranstiStopOrStreetEdge(StreetEdge wantedPath) {
        this.wantedPath = wantedPath;
    }

    @Override
    public String toString() {
        if (transitStop != null) {
            return "V: " + transitStop.getStop().getName() + " (" + transitStop.getStopId().getId() + ")";
        } else {
            return "E: " + wantedPath.getName() + 
                    " [" + wantedPath.getPermission() + "]";// +
                    //" (" + wantedPath.getLabel() +")";
        }
    }
    
    public boolean isTransitStop() {
        return transitStop != null;
    }
    
    public boolean isStreetEdge() {
        return wantedPath != null;
    }
    
    
}
