/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentripplanner.graph_builder.impl;

import org.opentripplanner.util.StreetType;
import com.vividsolutions.jts.geom.Point;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.TransitStopConnToWantedEdge;

/**
 *
 * @author mabu
 */
public class TransitToStreetConnection extends TransitStopConnToWantedEdge{
    
    private transient StreetTransitLink streetTransitLink;

    private TransitStopConnToWantedEdge toSuper() {
        return new TransitStopConnToWantedEdge(transitStop, wantedPath, streetType);
    }
    
    enum CollectionType {
        TRANSIT_LINK,
        WANTED_LINK
    }

    public TransitToStreetConnection(TransitStop transitStop, StreetTransitLink streetTransitLink, StreetEdge streetEdge, StreetType streetType) {
        super(transitStop, streetEdge, streetType);
        this.streetTransitLink = streetTransitLink;
    }    
    
    public static StreetFeatureCollection toFeatureCollection(List<TransitToStreetConnection> transitToStreetConnections, CollectionType collectionType) {
        List<StreetFeature> streetFeatures = new ArrayList<>(transitToStreetConnections.size());
        for(TransitToStreetConnection transitToStreetConnection: transitToStreetConnections) {
            streetFeatures.addAll(transitToStreetConnection.toStreetFeature(collectionType));
        }
        return new StreetFeatureCollection(streetFeatures);
    }
    
    private void addColor(StreetFeature sf, String propertyName) {
        switch (streetType) {
                case WALK_BIKE:
                    sf.addPropertie(propertyName, "#00ff00");
                    break;
                case NORMAL:
                    sf.addPropertie(propertyName, "#ff0000");
                    break;
                case SERVICE:
                    sf.addPropertie(propertyName, "#ff7f00");
                    break;
            }
    }
    
    public static List<TransitStopConnToWantedEdge> toSuper(List<TransitToStreetConnection> transitToStreetConnections) {
        List <TransitStopConnToWantedEdge> toWantedEdges = new ArrayList<>(transitToStreetConnections.size());
        for (TransitToStreetConnection ttsc : transitToStreetConnections) {
            toWantedEdges.add(ttsc.toSuper());
        }
        return toWantedEdges;
    }

    private List<StreetFeature> toStreetFeature(CollectionType collectionType) {
        List <StreetFeature> curFeatures = new ArrayList<>(3);
        if (collectionType == CollectionType.TRANSIT_LINK) {
            //Adds street transit link
            StreetFeature sf = StreetFeature.createRoadFeature(streetTransitLink);
            addColor(sf, "stroke");
            curFeatures.add(sf);
        } else if (collectionType == CollectionType.WANTED_LINK) {
            //Adds bus stop marker
            StreetFeature bus_stop_feat = new StreetFeature(GeometryUtils.getGeometryFactory().createPoint(transitStop.getCoordinate()));
            bus_stop_feat.addPropertie("title", transitStop.getName());
            bus_stop_feat.addPropertie("label", transitStop.getLabel());
            bus_stop_feat.addPropertie("stop_index", transitStop.getIndex());
            bus_stop_feat.addPropertie("edge_label", wantedPath.getLabel());
            bus_stop_feat.addPropertie("marker-size", "small");
            bus_stop_feat.addPropertie("marker-symbol", "bus");
            addColor(bus_stop_feat, "marker-color");
            curFeatures.add(bus_stop_feat);
            
            //and wanted/connected street edge which should be connected to this bus stop
            StreetFeature wanted_edge_feat = new StreetFeature(wantedPath.getGeometry());
            wanted_edge_feat.addPropertie("title", wantedPath.getName());
            wanted_edge_feat.addPropertie("label", wantedPath.getLabel());
            wanted_edge_feat.addPropertie("id", wantedPath.getId());
            wanted_edge_feat.addPropertie("stop_index", transitStop.getIndex());
            addColor(wanted_edge_feat, "stroke");
            curFeatures.add(wanted_edge_feat);
        }
        return curFeatures;
    }
    
}
