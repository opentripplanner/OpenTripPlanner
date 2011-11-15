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

package org.opentripplanner.api.ws;

import java.util.HashSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.services.GraphService;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

@XmlRootElement
public class GraphMetadata {


    /**
     * The bounding box of the graph, in decimal degrees.
     */
    private double lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude;

    @XmlElement
    private HashSet<TraverseMode> transitModes = new HashSet<TraverseMode>();

    public GraphMetadata() {
    }

    public GraphMetadata(GraphService graphService) {
        setGraphService(graphService);
    }

    @Autowired    
    public void setGraphService(GraphService graphService) {

        Graph graph = graphService.getGraph();
        
        /* generate extents */
        Envelope leftEnv = new Envelope();
        Envelope rightEnv = new Envelope();
        double aRightCoordinate = 0;
        for (GraphVertex gv : graph.getVertices()) {
            for (Edge e: gv.getOutgoing()) {
                if (e instanceof PatternHop) {
                    modes.add(((PatternHop) e).getMode());
                }
            }
            Coordinate c = gv.vertex.getCoordinate();
            if (c.x < 0) {
                leftEnv.expandToInclude(c);
            } else {
                rightEnv.expandToInclude(c);
                aRightCoordinate = c.x;
            }
        }

        if (leftEnv.getArea() == 0) {
            //the entire area is in the eastern hemisphere
            setLowerLeftLongitude(rightEnv.getMinX());
            setUpperRightLongitude(rightEnv.getMaxX());
            setLowerLeftLatitude(rightEnv.getMinY());
            setUpperRightLatitude(rightEnv.getMaxY());
        } else if (rightEnv.getArea() == 0) {
            //the entire area is in the western hemisphere
            setLowerLeftLongitude(leftEnv.getMinX());
            setUpperRightLongitude(leftEnv.getMaxX());
            setLowerLeftLatitude(leftEnv.getMinY());
            setUpperRightLatitude(leftEnv.getMaxY());
        } else {
            //the area spans two hemispheres.  Either it crosses the prime meridian,
            //or it crosses the 180th meridian (roughly, the international date line).  We'll check a random
            //coordinate to find out

            if (aRightCoordinate < 90) {
                //assume prime meridian
                setLowerLeftLongitude(leftEnv.getMinX());
                setUpperRightLongitude(rightEnv.getMaxX());
            } else {
                //assume 180th meridian
                setLowerLeftLongitude(leftEnv.getMaxX());
                setUpperRightLongitude(rightEnv.getMinX());
            }
            setUpperRightLatitude(Math.max(rightEnv.getMaxY(), leftEnv.getMaxY()));
            setLowerLeftLatitude(Math.min(rightEnv.getMinY(), leftEnv.getMinY()));
        }
    }

    public void setLowerLeftLatitude(double lowerLeftLatitude) {
        this.lowerLeftLatitude = lowerLeftLatitude;
    }

    public double getLowerLeftLatitude() {
        return lowerLeftLatitude;
    }

    public void setUpperRightLatitude(double upperRightLatitude) {
        this.upperRightLatitude = upperRightLatitude;
    }

    public double getUpperRightLatitude() {
        return upperRightLatitude;
    }

    public void setUpperRightLongitude(double upperRightLongitude) {
        this.upperRightLongitude = upperRightLongitude;
    }

    public double getUpperRightLongitude() {
        return upperRightLongitude;
    }

    public void setLowerLeftLongitude(double lowerLeftLongitude) {
        this.lowerLeftLongitude = lowerLeftLongitude;
    }

    public double getLowerLeftLongitude() {
        return lowerLeftLongitude;
    }

    /**
     * The bounding box of the graph, in decimal degrees.  These are the old, deprecated
     * names; the new names are the lowerLeft/upperRight.
     *  @deprecated
     */
    public void setMinLatitude(double minLatitude) {
        lowerLeftLatitude = minLatitude;
    }

    /**
     * The bounding box of the graph, in decimal degrees.  These are the old, deprecated
     * names; the new names are the lowerLeft/upperRight.
     *  @deprecated
     */
    public double getMinLatitude() {
        return lowerLeftLatitude;
    }

    /**
     * The bounding box of the graph, in decimal degrees.  These are the old, deprecated
     * names; the new names are the lowerLeft/upperRight.
     *  @deprecated
     */
    public void setMinLongitude(double minLongitude) {
        lowerLeftLongitude = minLongitude;
    }

    /**
     * The bounding box of the graph, in decimal degrees.  These are the old, deprecated
     * names; the new names are the lowerLeft/upperRight.
     *  @deprecated
     */
    public double getMinLongitude() {
        return lowerLeftLongitude;
    }

    /**
     * The bounding box of the graph, in decimal degrees.  These are the old, deprecated
     * names; the new names are the lowerLeft/upperRight.
     *  @deprecated
     */
    public void setMaxLatitude(double maxLatitude) {
        upperRightLatitude = maxLatitude;
    }

    /**
     * The bounding box of the graph, in decimal degrees.  These are the old, deprecated
     * names; the new names are the lowerLeft/upperRight.
     *  @deprecated
     */
    public double getMaxLatitude() {
        return upperRightLatitude;
    }

    /**
     * The bounding box of the graph, in decimal degrees.  These are the old, deprecated
     * names; the new names are the lowerLeft/upperRight.
     *  @deprecated
     */
    public void setMaxLongitude(double maxLongitude) {
        upperRightLongitude = maxLongitude;
    }

    /**
     * The bounding box of the graph, in decimal degrees.  These are the old, deprecated
     * names; the new names are the lowerLeft/upperRight.
     *  @deprecated
     */
    public double getMaxLongitude() {
        return upperRightLongitude;
    }

    public HashSet<TraverseMode> getTransitModes() {
        return transitModes;
    }

    public void setTransitModes(HashSet<TraverseMode> transitModes) {
        this.transitModes = transitModes;
    }
}
