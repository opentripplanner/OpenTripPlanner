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
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.impl.DistanceLibrary;

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
     * The distance between samples in meters. Defaults to 10m, the approximate resolution of 1/3
     * arc-second NED data.
     */
    private double distanceBetweenSamplesM = 10;

    public void setGridCoverageFactory(NEDGridCoverageFactory factory) {
        gridCoverageFactory = factory;
    }

    public void setDistanceBetweenSamplesM(double distance) {
        distanceBetweenSamplesM = distance;
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

        for (GraphVertex gv : graph.getVertices()) {
            for (Edge ee : gv.getOutgoing()) {
                if (ee instanceof TurnEdge) {
                    processEdge((TurnEdge) ee);
                }
            }
        }

    }

    /**
     * Processes a single {@link Street} edge, creating and assigning the elevation profile.
     * 
     * @param st the street edge
     */
    private void processEdge(TurnEdge st) {
        
        Geometry g = (Geometry) st.getGeometry();
        Coordinate[] coords = g.getCoordinates();
        
        List<Coordinate> coordList = new LinkedList<Coordinate>();

        // calculate the total edge length in meters
        double edgeLenM = 0;
        for (int i = 0; i < coords.length-1; i++) {
            edgeLenM += DistanceLibrary.distance(coords[i].y, coords[i].x, coords[i+1].y, coords[i+1].x);
        }
        
        // initial sample (x = 0)
        coordList.add(new Coordinate(0, getElevation(coords[0])));

        // loop for edge-internal samples
        for (double x = distanceBetweenSamplesM; x < edgeLenM; x += distanceBetweenSamplesM) {
            // avoid final-segment samples less than half the distance between samples:
            if (edgeLenM-x < distanceBetweenSamplesM/2) {
                break;
            }
            
            Coordinate internal = getPointAlongEdge(coords, edgeLenM, x/edgeLenM);
            coordList.add(new Coordinate(x, getElevation(internal)));    
        }

        // final sample (x = edge length)
        coordList.add(new Coordinate(edgeLenM, getElevation(coords[coords.length-1])));            

        // construct the PCS
        Coordinate coordArr[] = new Coordinate[coordList.size()];
        PackedCoordinateSequence elevPCS = new PackedCoordinateSequence.Double(coordList
                .toArray(coordArr));
        
        ((StreetVertex) st.getFromVertex()).setElevationProfile(elevPCS);
            
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
        
        for (int i = 1; i < coords.length-1; i++) { //loop through inner points
          Coordinate innerPt = coords[i];
          x2 = innerPt.x;
          y2 = innerPt.y;
          
          // percentage of total edge length represented by current segment:
          double pct = DistanceLibrary.distance(y1, x1, y2, x2) / length;

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

        double pct = DistanceLibrary.distance(y1, x1, y2, x2) / length;
        double pctAlongSeg = (t - pctThrough)/pct;
        
        return new Coordinate(x1 + (pctAlongSeg * (x2 - x1)), y1 + (pctAlongSeg * (y2 - y1)));
      }

   
    /**
     * Method for retrieving the elevation at a given Coordinate.
     * 
     * @param c the coordinate (NAD83)
     * @return elevation in meters
     */
    private double getElevation(Coordinate c) {
        return getElevation(c.x, c.y);
    }
    
    /**
     * Method for retrieving the elevation at a given (x, y) pair.
     * 
     * @param x the query longitude (NAD83)
     * @param y the query latitude (NAD83)
     * @return elevation in meters
     */
    private double getElevation(double x, double y) {
        double values[] = new double[1];
        try {
            coverage.evaluate(new DirectPosition2D(x, y), values);
        } catch (org.opengis.coverage.PointOutsideCoverageException e) {
            //skip this for now
        }
        return values[0];
    }

}