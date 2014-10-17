/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.graph_builder.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import org.opentripplanner.routing.edgetype.StreetTransitLink;

/**
 *
 * @author mabu
 */
@XmlRootElement(name = "GeoStreet")
public class StreetFeature {
    
    @JsonSerialize
    private final String type = "Feature";

    public String getType() {
        return type;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
    
    
    
    @JsonCreator
    public StreetFeature(
            @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("properties") Map<String, Object> properties) {
        this.geometry = geometry;
        this.properties = properties;
    }
    
    public StreetFeature(Geometry geometry) {
        this.geometry = geometry;
        this.properties = new HashMap<>(10);
    }
    
    public static StreetFeature createPolygonFeature(String name, Geometry geometry) {
        Map<String, Object> properties = new HashMap<String, Object>(10);
        properties.put("name", name);
        properties.put("stroke", "#FFFF00");
        properties.put("fill", "#FFFF00");
        return new StreetFeature(geometry, properties);
    }
    
    public static StreetFeature createCycleFeature(String name, String permissions, LineString geometry) {
        Map<String, Object> properties = new HashMap<String, Object>(10);
        properties.put("name", name);
        properties.put("permissions", permissions);
        properties.put("stroke", "#00FF00");
        return new StreetFeature(geometry, properties);
    }
    
    public static StreetFeature createRoadFeature(String name, String permissions, LineString geometry) {
        Map<String, Object> properties = new HashMap<String, Object>(10);
        properties.put("name", name);
        properties.put("permissions", permissions);
        properties.put("stroke", "#0000FF");
        return new StreetFeature(geometry, properties);
    }
    
    public static StreetFeature createRoadFeature(StreetTransitLink streetTransitLink) {
        Map<String, Object> properties = new HashMap<String, Object>(10);
        properties.put("name", streetTransitLink.getFromVertex().getName());
        properties.put("label", streetTransitLink.getFromVertex().getLabel());
        properties.put("link_id", Integer.toString(streetTransitLink.getId()));
        properties.put("stop_index", Integer.toString(streetTransitLink.getFromVertex().getIndex()));
        return new StreetFeature(streetTransitLink.getGeometry(), properties);
    }
    
    
    
    public void addPropertie(String name, Object value) {
        this.properties.put(name, value);
    }
    
    
    //@JsonSerialize(using=GeoJSONSerializer.class)
    //@JsonDeserialize(using=GeoJSONDeserializer.class)
    //@XmlJavaTypeAdapter(value=GeometryAdapter.class,type=Geometry.class)
    private Geometry geometry;
    
    @JsonSerialize
    private final Map<String, Object> properties;
}
