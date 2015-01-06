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
package org.opentripplanner.graph_builder.impl;

import org.opentripplanner.util.StreetType;
import com.vividsolutions.jts.geom.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    //Is transitStop linked to the same edge as in correctly linked edges
    private Boolean correctlyLinked;

    /**
     * Converts to {@link TransitStopConnToWantedEdge} which is used in serialization
     * 
     * @return new {@link TransitStopConnToWantedEdge} with same data as current
     * @see #toSuper(java.util.List) 
     */
    private TransitStopConnToWantedEdge toSuper() {
        return new TransitStopConnToWantedEdge(transitStop, wantedPath, streetType);
    }
    
    enum CollectionType {
        TRANSIT_LINK,
        WANTED_LINK,
        CORRECT_LINK
    }

    public TransitToStreetConnection(TransitStop transitStop, StreetTransitLink streetTransitLink, StreetEdge streetEdge, StreetType streetType) {
        super(transitStop, streetEdge, streetType);
        this.streetTransitLink = streetTransitLink;
        this.correctlyLinked = null;
    }
    
    public TransitToStreetConnection(TransitStopConnToWantedEdge transitStopConnToWantedEdge, StreetTransitLink streetTransitLink, boolean correctlyLinked) {
        super(transitStopConnToWantedEdge.getTransitStop(), transitStopConnToWantedEdge.getStreetEdge(), transitStopConnToWantedEdge.getStreetType());
        this.streetTransitLink = streetTransitLink;
        this.correctlyLinked = correctlyLinked;
    }
    
    /**
     * Generates {@link StreetFeatureCollection} which can be easily serialized to Geojson Feature collection
     * 
     * Uses {@link #toStreetFeature(org.opentripplanner.graph_builder.impl.TransitToStreetConnection.CollectionType)} 
     * @param transitToStreetConnections List of connections
     * @param collectionType Type of featureCollection
     * @return
     * @throws Exception 
     * @see #toStreetFeature(org.opentripplanner.graph_builder.impl.TransitToStreetConnection.CollectionType) 
     */
    public static StreetFeatureCollection toFeatureCollection(List<TransitToStreetConnection> transitToStreetConnections, CollectionType collectionType) throws Exception {
        List<StreetFeature> streetFeatures = new ArrayList<>(transitToStreetConnections.size());
        for(TransitToStreetConnection transitToStreetConnection: transitToStreetConnections) {
            streetFeatures.addAll(transitToStreetConnection.toStreetFeature(collectionType));
        }
        return new StreetFeatureCollection(streetFeatures);
    }
    
    /**
     * Sets color of Geojson Feature based on {@link StreetType}
     * 
     * Colors are:
     * - green for walkable/bikable ways
     * - orange for service ways
     * - red for everything else
     * 
     * @param sf to which {@link StreetFeature} should color be set
     * @param propertyName for which property should color be set (stroke,marker-color,etc)
     */
    private void addColorStreetType(StreetFeature sf, String propertyName) {
        switch (streetType) {
                case WALK_BIKE:
                    //green for connections which are connected to walkable/bikable ways
                    sf.addPropertie(propertyName, "#00ff00");
                    break;
                case NORMAL:
                    //red for everything else
                    sf.addPropertie(propertyName, "#ff0000");
                    break;
                case SERVICE:
                    //orange for connections to service ways
                    sf.addPropertie(propertyName, "#ff7f00");
                    break;
            }
    }
    
    /**
     * Sets color of Geojson Feature based on correctlyLinked
     * 
     * Colors are: (green - correct, red - incorect)
     * 
     * @param sf to which {@link StreetFeature} should color be set
     * @param propertyName for which property should color be set (stroke,marker-color,etc)
     */
    private void addColorCoretness(StreetFeature sf, String propertyName) {
        if (correctlyLinked) {
            //green
            sf.addPropertie(propertyName, "#00ff00");
        } else {
            //red
            sf.addPropertie(propertyName, "#ff0000");
        }
    }
    
    /**
     * Returns list of {@link TransitStopConnToWantedEdge} with same info
     * as current list sorted according to stop name
     * 
     * Used when saving list of connections for checking them in vizGui
     * 
     * @param transitToStreetConnections List to be saved
     * @return
     * @see #toSuper() 
     */
    public static List<TransitStopConnToWantedEdge> toSuper(List<TransitToStreetConnection> transitToStreetConnections) {
        List <TransitStopConnToWantedEdge> toWantedEdges = new ArrayList<>(transitToStreetConnections.size());
        //Sort according to stop name
        Collections.sort(transitToStreetConnections, new Comparator<TransitStopConnToWantedEdge>() {

            @Override
            public int compare(TransitStopConnToWantedEdge o1, TransitStopConnToWantedEdge o2) {
                return o1.getTransitStop().getName().compareTo(o2.getTransitStop().getName());
            }
        });
         for (TransitToStreetConnection ttsc : transitToStreetConnections) {
            toWantedEdges.add(ttsc.toSuper());
        }
        return toWantedEdges;
    }

    /**
     * Generates {@link StreetFeature} from this to be serialized into GeojsonFeature
     * 
     * There are 3 types of Features that can be generated:
     * - TRANSIT_LINK (outputs line {@link StreetTransitLink} with color {@link #addColorStreetType(org.opentripplanner.graph_builder.impl.StreetFeature, java.lang.String) }
     * - WANTED_LINK (outputs point TransitStop and line wantedEdge both with color {@link #addColorStreetType(org.opentripplanner.graph_builder.impl.StreetFeature, java.lang.String) } 
     * - CORRECT_LINK (outputs point TransitStop and line wantedEdge both with color {@link #addColorCoretness(org.opentripplanner.graph_builder.impl.StreetFeature, java.lang.String) }
     * @param collectionType Type of streetFeature
     * @return List of StreetFeatures
     * @throws Exception 
     * @see #addColorCoretness(org.opentripplanner.graph_builder.impl.StreetFeature, java.lang.String) 
     * @see #addColorStreetType(org.opentripplanner.graph_builder.impl.StreetFeature, java.lang.String) 
     */
    private List<StreetFeature> toStreetFeature(CollectionType collectionType) throws Exception {
        List <StreetFeature> curFeatures = new ArrayList<>(3);
        if (collectionType == CollectionType.TRANSIT_LINK) {
            //Adds street transit link
            StreetFeature sf = StreetFeature.createRoadFeature(streetTransitLink);
            addColorStreetType(sf, "stroke");
            curFeatures.add(sf);
        } else if (collectionType == CollectionType.WANTED_LINK) {
            //Adds bus stop marker
            StreetFeature bus_stop_feat = new StreetFeature(GeometryUtils.getGeometryFactory().createPoint(transitStop.getCoordinate()));
            bus_stop_feat.addPropertie("title", transitStop.getName() + "(" + transitStop.getStopId().getId() + ")");
            bus_stop_feat.addPropertie("label", transitStop.getLabel());
            //bus_stop_feat.addPropertie("stop_index", transitStop.getIndex());
            bus_stop_feat.addPropertie("edge_label", wantedPath.getName());
            bus_stop_feat.addPropertie("marker-size", "small");
            bus_stop_feat.addPropertie("marker-symbol", "bus");
            addColorStreetType(bus_stop_feat, "marker-color");
            curFeatures.add(bus_stop_feat);
            
            //and wanted/connected street edge which should be connected to this bus stop
            StreetFeature wanted_edge_feat = new StreetFeature(wantedPath.getGeometry());
            wanted_edge_feat.addPropertie("title", street_name);
            wanted_edge_feat.addPropertie("label", wantedPath.getName());
            //wanted_edge_feat.addPropertie("id", wantedPath.getId());
            //wanted_edge_feat.addPropertie("stop_index", transitStop.getIndex());
            wanted_edge_feat.addPropertie("stop_label", transitStop.getLabel());
            addColorStreetType(wanted_edge_feat, "stroke");
            curFeatures.add(wanted_edge_feat);
        } else if (collectionType == CollectionType.CORRECT_LINK) {
            if (correctlyLinked == null) {
                throw new Exception("For CORRECT_LINK feature correctlyLinked parameter can't be null!");
            }
            //Adds bus stop marker
            StreetFeature bus_stop_feat = new StreetFeature(GeometryUtils.getGeometryFactory().createPoint(transitStop.getCoordinate()));
            bus_stop_feat.addPropertie("title", transitStop.getName() + "(" + transitStop.getStopId().getId() + ")");
            bus_stop_feat.addPropertie("label", transitStop.getLabel());
            //bus_stop_feat.addPropertie("stop_index", transitStop.getIndex());
            bus_stop_feat.addPropertie("edge_label", wantedPath.getName());
            bus_stop_feat.addPropertie("marker-size", "small");
            bus_stop_feat.addPropertie("marker-symbol", "bus");
            addColorCoretness(bus_stop_feat, "marker-color");
            curFeatures.add(bus_stop_feat);
            
            //and wanted/connected street edge which should be connected to this bus stop
            StreetFeature wanted_edge_feat = new StreetFeature(wantedPath.getGeometry());
            wanted_edge_feat.addPropertie("title", street_name);
            wanted_edge_feat.addPropertie("label", wantedPath.getName());
            //wanted_edge_feat.addPropertie("id", wantedPath.getId());
            wanted_edge_feat.addPropertie("stop_label", transitStop.getLabel());
            addColorCoretness(wanted_edge_feat, "stroke");
            curFeatures.add(wanted_edge_feat);
            
        }
        return curFeatures;
    }
    
}
