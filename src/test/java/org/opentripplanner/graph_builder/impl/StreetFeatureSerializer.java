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
import java.io.IOException;

/**
 *
 * @author mabu
 */
public class StreetFeatureSerializer extends JsonSerializer<StreetFeature> {

    @Override
    public void serialize(StreetFeature v, JsonGenerator jgen, SerializerProvider sp) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("type", v.getType());
        jgen.writeFieldName("geometry");
        jgen.writeObject(v.getGeometry());
        
        jgen.writeFieldName("properties");
        jgen.writeObject(v.getProperties());
        jgen.writeEndObject();
    }

    @Override
    public Class<StreetFeature> handledType() {
        return StreetFeature.class;
    }
    
    
    
}
