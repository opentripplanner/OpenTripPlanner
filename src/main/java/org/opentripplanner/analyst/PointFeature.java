package org.opentripplanner.analyst;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.geojson.Feature;
import org.opentripplanner.common.geometry.GeometryUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PointFeature implements Serializable {

    private static final long serialVersionUID = -613136927314702334L;

    private static final ObjectMapper deserializer = new ObjectMapper();
    
    private String id;
    private Geometry geom;
    private Map<String,Integer> properties;
    private double lat;
    private double lon;

    public PointFeature(){
        // blank constructor for deserialization
        this(null);
    }

    public PointFeature(String id){
        this.id = id;
        this.geom = null;
        this.properties = new HashMap<String,Integer>();
    }

    public PointFeature(String id, Geometry g,  HashMap<String,Integer> ad) throws EmptyPolygonException, UnsupportedGeometryException{
        this.id = id;
        this.setGeom(g);
        this.properties = ad;
    }

    public void addAttribute( String id, Integer val ){
        this.properties.put(id, val);
    }

    public void setGeom(Geometry geom) throws EmptyPolygonException, UnsupportedGeometryException {
        if (geom instanceof MultiPolygon) {
            if (geom.isEmpty()) {
                throw new EmptyPolygonException();
            }
            if (geom.getNumGeometries() > 1) {
                // LOG.warn("Multiple polygons in MultiPolygon, using only the first.");
                // TODO percolate this warning up somehow
            }
            this.geom = geom.getGeometryN(0);
        } else if( geom instanceof Point || geom instanceof Polygon){
            this.geom = geom;
        } else {
            throw new UnsupportedGeometryException( "Non-point, non-polygon Geometry, not supported." );
        }

        // cache a representative point
        Point point = geom.getCentroid();
        this.lat = point.getY();
        this.lon = point.getX();
    }

    public Polygon getPolygon(){
        if( geom instanceof Polygon ){
            return (Polygon)geom;
        } else {
            return null;
        }
    }

    public Geometry getGeom() {
        return geom;
    }

    public Map<String,Integer> getProperties() {
        return properties;
    }

    public String getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public static PointFeature fromJsonNode(JsonNode feature) throws EmptyPolygonException,
            UnsupportedGeometryException {
        Feature geoJsonFeature;
        try {
            geoJsonFeature = deserializer.readValue(feature.traverse(), Feature.class);
        } catch (IOException e) {
            throw new UnsupportedGeometryException(e.getMessage());
        }
        PointFeature ret = new PointFeature(geoJsonFeature.getId());
        ret.setGeom(GeometryUtils.convertGeoJsonToJtsGeometry(geoJsonFeature.getGeometry()));
        Object structured = geoJsonFeature.getProperty("structured");
        if (structured == null || !(structured instanceof Map))
            return null;
        // The code below assume the structured map to have integers only
        ret.setAttributes((Map<String, Integer>)(structured));
        return ret;
    }

    private void setAttributes(Map<String,Integer> properties) {
        this.properties = properties;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLat() {
        return this.lat;
    }

    public double getLon() {
        return this.lon;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getProperty(String id) {
        return this.properties.get(id);
    }
    
    /**
     * Compare to another object.
     * 
     * We can't use identity equality, because point features may be serialized and deserialized
     * and thus the same PointFeature may exist in memory more than once. For example, PointFeatures
     * are compared inside the conveyal/otpa-cluster project to figure out which origins have
     * returned from the compute cluster. 
     */
    public boolean equals (Object o) {
        if (o instanceof PointFeature) {
            PointFeature f = (PointFeature) o;
            return f.lat == this.lat &&
                    f.lon == this.lon &&
                    (f.geom == this.geom || f.geom != null && f.geom.equals(this.geom)) &&
                    (f.id == this.id || f.id != null && f.id.equals(this.id)) &&
                    this.properties.equals(f.properties);
        }
        
        return false; 
    }
    
    /**
     * Hash the relevant features of this PointFeature for efficient use in HashSets, etc.
     * PointFeatures are put in HashSets in the conveyal/otpa-cluster project.
     */
    public int hashCode () {
        return (int) (this.lat * 1000) + (int) (this.lon * 1000) +
                (this.geom != null ? this.geom.hashCode() : 0) + 
                (this.id != null ? this.id.hashCode() : 0) +
                this.properties.hashCode();
    }
}
