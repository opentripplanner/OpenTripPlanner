/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.impl.ned;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import javax.media.jai.InterpolationBilinear;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.coverage.Coverage;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.ned.NEDGridCoverageFactory;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * {@link GraphBuilder} plugin that takes a constructed (@link Graph} and overlays it onto National
 * Elevation Dataset (NED) raster data, creating elevation profiles for each Street encountered in
 * the Graph. The elevation profile is stored as a {@link PackedCoordinateSequence}, where each
 * (x,y) pair represents one sample, with the x-coord representing the distance along the edge as,
 * measured from the start, and the y-coord representing the sampled elevation at that point (both
 * in meters).
 * 
 * @author demory
 * 
 */
public class NEDGraphBuilderImpl implements GraphBuilder {

    private NEDGridCoverageFactory gridCoverageFactory;

    private Coverage coverage;

    /**
     * The sampling frequency in meters. Defaults to 10m, the approximate resolution of 1/3 arc-second
     * NED data.
     */
    private double sampleFreqM = 10;

    public void setGridCoverageFactory(NEDGridCoverageFactory factory) {
        gridCoverageFactory = factory;
    }

    public void setSampleFrequency(double freq) {
        sampleFreqM = freq;
    }

    @Override
    public void buildGraph(Graph graph) {

        Coverage gridCov = gridCoverageFactory.getGridCoverage();
        
        // If gridCov is a GridCoverage2D, apply a bilinear interpolator. Otherwise, just use the
        // coverage as is (note: UnifiedGridCoverages created by NEDGridCoverageFactoryImpl handle
        // interpolation internally)           
        coverage = (gridCov instanceof GridCoverage2D) ? 
                Interpolator2D.create((GridCoverage2D) gridCov, new InterpolationBilinear()) :
                gridCov;

        for (Vertex vv : graph.getVertices()) {
            for (Edge ee : vv.getOutgoing()) {
                if (ee instanceof Street) {
                    processEdge((Street) ee);
                }
            }
        }

    }

    /**
     * Processes a single {@link Street} edge, creating and assigning the elevation profile.
     * 
     * @param st the street edge
     */
    private void processEdge(Street st) {
        
        Geometry g = (Geometry) st.getGeometry();
        Coordinate[] coords = g.getCoordinates();
        
        List<Coordinate> coordList = new LinkedList<Coordinate>();

        // calculate the total edge length in m and degrees
        double edgeLenD=0, edgeLenM = 0;
        for (int i = 0; i < coords.length-1; i++) {
            edgeLenD += Point2D.distance(coords[i].x, coords[i].y, coords[i+1].x, coords[i+1].y);
            edgeLenM += haversine(coords[i].x, coords[i].y, coords[i+1].x, coords[i+1].y);
        }
        
        // initial sample (x = 0)
        coordList.add(new Coordinate(0, getElevation(coords[0])));            

        // loop for edge-internal samples
        for(double x = sampleFreqM; x < edgeLenM; x += sampleFreqM) {
            // avoid final-segment samples less than half the frequency length:
            if(edgeLenM-x < sampleFreqM/2) continue;
            
            Coordinate internal = getPointAlongEdge(coords, edgeLenD, x/edgeLenM);
            coordList.add(new Coordinate(x, getElevation(internal)));            
        }

        // final sample (x = edge length)
        //System.out.println("final sample x="+edgeLenM+" coord="+coords[coords.length-1]);
        coordList.add(new Coordinate(edgeLenM, getElevation(coords[coords.length-1])));            

        // construct the PCS
        Coordinate coordArr[] = new Coordinate[coordList.size()];
        PackedCoordinateSequence elevPCS = new PackedCoordinateSequence.Double(coordList
                .toArray(coordArr));

        //for (Coordinate c : elevPCS.toCoordinateArray()) System.out.println(" "+c.toString());

        st.setElevationProfile(elevPCS);
            
    }

    /**
     * Returns a coordinate along a path located at a specific point indicated by the percentage
     * of distance covered from start to end.
     * 
     * @param coords the list of (x,y) coordinates that form the path
     * @param length the total length of the path 
     * @param t the percentage (ranges from 0 to 1)
     * @return the (x,y) coordinate at t
     */
    public Coordinate getPointAlongEdge(Coordinate[] coords, double length, double t) {
        
        double pctThrough = 0; // current percentage of the edge length traversed
        
        // endpoints of current segment within edge:
        double x1 = coords[0].x, y1 = coords[0].y, x2, y2;
        
        for(int i = 1; i < coords.length-1; i++) { //loop through inner points
          Coordinate innerPt = coords[i];
          x2 = innerPt.x;
          y2 = innerPt.y;
          
          // percentage of total edge length represented by current sigment:
          double pct = Point2D.distance(x1, y1, x2, y2) / length;

          if (pctThrough + pct > t) { // if current segment contains 't,' we're done
            double pctAlongSeg = (t - pctThrough) / pct;
            return new Coordinate(x1 + (pctAlongSeg * (x2 - x1)), y1 + (pctAlongSeg * (y2 - y1)));
          }

          pctThrough += pct;
          x1 = x2;
          y1 = y2;
        }
        
        // handle the final segment separately
        x2 = coords[coords.length-1].x;
        y2 = coords[coords.length-1].y;

        double pct = Point2D.distance(x1, y1, x2, y2) / length;
        double pctAlongSeg = (t - pctThrough)/pct;
        
        return new Coordinate(x1 + (pctAlongSeg * (x2 - x1)), y1 + (pctAlongSeg * (y2 - y1)));
      }


    /**
     * Uses the haversine formula to calculate the distance in meters between two lat/lon
     * coordinates.
     * 
     * @param lon1 The start longitude in decimal degrees
     * @param lat1 The start latitude in decimal degrees
     * @param lon2 The end longitude in decimal degrees
     * @param lat2 The end latitude in decimal degrees
     * @return the length in meters along the earth's surface
     */
    private double haversine(double lon1, double lat1, double lon2, double lat2) {
        double r = 6378137;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1); 
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * 
                Math.sin(dLon/2) * Math.sin(dLon/2); 
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
        return r * c;
    }

   
    /**
     * Method for retreiving the elevation at a given Coordinate.
     * 
     * @param c the coordinate (NAD83)
     * @return elevation in meters
     */
    private double getElevation(Coordinate c) {
        return getElevation(c.x, c.y);
    }
    
    /**
     * Method for retreiving the elevation at a given (x, y) pair.
     * 
     * @param x the query longitude (NAD83)
     * @param y the query latitude (NAD83)
     * @return elevation in meters
     */
    private double getElevation(double x, double y) {
        double values[] = new double[1];
        coverage.evaluate(new DirectPosition2D(x, y), values);
        return values[0];
    }

}