/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opentripplanner.graph_builder.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 *
 * @author mabu
 */
class GeoJsonEdgeListSerializer extends JsonSerializer<List<Edge>> {

    @Override
    public void serialize(List<Edge> value, JsonGenerator jgen, SerializerProvider sp) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("type", "FeatureCollection");
        jgen.writeArrayFieldStart("features");
        for(Edge e: value) {
            //writeEdge(jgen, e);
        }
        jgen.writeEndArray();
        jgen.writeEndObject();
    }

    public static void writeEdge(JsonGenerator jgen, Edge e, Boolean hasWalkPath) throws IOException {
        jgen.writeStartObject();
        jgen.writeFieldName("geometry");
        jgen.writeObject(e.getGeometry());
        jgen.writeStringField("type", "Feature");
        jgen.writeStringField("id", Integer.toString(e.getId()));
        jgen.writeObjectFieldStart("properties");
        jgen.writeStringField("name", e.getFromVertex().getName());
        jgen.writeStringField("link_id", Integer.toString(e.getId()));
        jgen.writeStringField("stop_index", Integer.toString(e.getFromVertex().getIndex()));
        if (hasWalkPath != null) {
            if (hasWalkPath) {
                jgen.writeStringField("stroke", "#00ff00");
            } else {
                jgen.writeStringField("stroke", "#ff0000");
            }
        }
        jgen.writeEndObject();   
        jgen.writeEndObject();
        //writeCoordinate(jgen, e.getFromVertex());
    }
    
    public static void writeCoordinate(JsonGenerator jgen, Vertex ts) throws IOException {
        jgen.writeStartObject();
        jgen.writeFieldName("geometry");
        writePoint(jgen, ts.getCoordinate());
        jgen.writeStringField("type", "Feature");
        
        jgen.writeObjectFieldStart("properties");
        jgen.writeStringField("name", ts.getName());
        jgen.writeStringField("index", Integer.toString(ts.getIndex()));
        jgen.writeStringField("label", ts.getLabel());
        jgen.writeEndObject();   
        jgen.writeEndObject();
    }
    
    public static void writePolygon(JsonGenerator jgen, Geometry geo, Edge e) throws IOException {
        jgen.writeStartObject();
        jgen.writeFieldName("geometry");
        jgen.writeObject(geo);
        jgen.writeStringField("type", "Feature");
        
        jgen.writeObjectFieldStart("properties");
        //jgen.writeStringField("name", ts.getName());
        jgen.writeStringField("stroke", "#FFFF00");
        jgen.writeStringField("fill", "#FFFF00");
        jgen.writeStringField("opacity", "0.6");
        jgen.writeStringField("link_id", Integer.toString(e.getId()));
        jgen.writeStringField("stop_index", Integer.toString(e.getFromVertex().getIndex()));
        jgen.writeStringField("name", e.getFromVertex().getName());
        
        jgen.writeEndObject();   
        jgen.writeEndObject();
    }
    
    
    private static void writePoint(JsonGenerator jgen, Coordinate p) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("type", "Point");
        jgen.writeFieldName("coordinates");
        
        jgen.writeStartArray();
        jgen.writeNumber(p.x);
        jgen.writeNumber(p.y);
        jgen.writeEndArray();

        jgen.writeEndObject();
    }

    @Override
    public Class<List<Edge>> handledType() {
        return (Class<List<Edge>>) Collections.<Edge>emptyList().getClass();
    }
    
    
    
}
