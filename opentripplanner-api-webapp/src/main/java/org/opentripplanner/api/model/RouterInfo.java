package org.opentripplanner.api.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opentripplanner.model.json_serialization.GeoJSONDeserializer;
import org.opentripplanner.model.json_serialization.GeoJSONSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Geometry;

@XmlRootElement(name = "RouterInfo")
public class RouterInfo {
    @XmlElement
    public String routerId;
    
    @JsonSerialize(using=GeoJSONSerializer.class)
    @JsonDeserialize(using=GeoJSONDeserializer.class)
    @XmlJavaTypeAdapter(value=GeometryAdapter.class,type=Geometry.class)
    public Geometry polygon;
}
