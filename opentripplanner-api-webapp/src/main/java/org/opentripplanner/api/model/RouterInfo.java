package org.opentripplanner.api.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.opentripplanner.api.model.json_serializers.GeoJSONDeserializer;
import org.opentripplanner.api.model.json_serializers.GeoJSONSerializer;

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
