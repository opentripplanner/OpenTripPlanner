package org.opentripplanner.routing.core;

import java.util.Comparator;
import java.util.List;

/**
 * A Comparator implementation that will sort a 1D list of Vertex objects, preserving
 * 2D spatial locality. It implicitly compares the Morton code for a crude equal-area 
 * projection of the vertex's lon/lat coordinates. 
 *
 * @author andrewbyrd
 */
public class MortonVertexComparator implements Comparator<Vertex> {

    private static final double METERS_PER_DEGREE_LAT = 111111.111111;

    private double meters_per_degree_lon;
    
    private double minLon;
    
    private double refLat;
    
    public MortonVertexComparator() {
        minLon = -180;
        setRefLat(45);
    }
        
    public MortonVertexComparator(List<Vertex> domain) {
        // Recenter the projection near the area of interest.
        // Comparator handles negative numbers poorly, so must scan for minimum 
        // longitude.
        setRefLat(domain.get(0).getY());
        minLon = Double.POSITIVE_INFINITY;
        for (Vertex v : domain)
            if (v.getX() < minLon)
                minLon = v.getX();
    }
    
    /* Specify the reference latitude for the projection.
     * It is tempting to scale the x units using avg latitude of the two points during each
     * comparison, but this will cause a vertex's projected x coordinate to vary slightly 
     * depending on which other vertex it is compared to. This noise ruins the comparison.
     */ 
    private void setRefLat(double lat) {
        refLat = lat;
        meters_per_degree_lon = METERS_PER_DEGREE_LAT * Math.cos(lat * Math.PI / 180.0);
    }
    
    @Override
    public int compare(Vertex v0, Vertex v1) {
        
        double lon0 = v0.getX() - minLon;
        double lat0 = v0.getY();
        double lon1 = v1.getX() - minLon;
        double lat1 = v1.getY();
        
        long x0 = (long) Math.abs(lon0 * meters_per_degree_lon);
        long y0 = (long) Math.abs(lat0 * METERS_PER_DEGREE_LAT);
        long x1 = (long) Math.abs(lon1 * meters_per_degree_lon);
        long y1 = (long) Math.abs(lat1 * METERS_PER_DEGREE_LAT);
        
        // mask higher order bits that are identical
        long dx = x0 ^ x1;
        long dy = y0 ^ y1;
        
        // determine which dimension has the most significant bit 
        if (dy < dx && (dy < (dx ^ dy))) {
            return (int) (x0 - x1);
        } else {
            return (int) (y0 - y1);
        }
    }
    
}