package org.opentripplanner.api.model;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.common.geometry.GeometryUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.gml2.GMLWriter;

public class GeometryAdapter extends XmlAdapter<String,Geometry> {
    public Geometry unmarshal(String val) throws Exception {
        return new WKTReader(GeometryUtils.getGeometryFactory()).read(val);
    }
    public String marshal(Geometry val) throws Exception {
        GMLWriter writer = new GMLWriter();
        writer.setNamespace(false);
        return writer.write(val);
    }
}