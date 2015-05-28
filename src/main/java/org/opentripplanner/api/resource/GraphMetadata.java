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

package org.opentripplanner.api.resource;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * GraphMetada is first created after OSM is build.
 *
 * It has two envelopes. One with all of OSM vertices. This is used in {@link org.opentripplanner.graph_builder.module.StreetLinkerModule} and
 * {@link org.opentripplanner.graph_builder.module.TransitToTaggedStopsModule} to skip stops which are outside OSM data area.
 *
 * During GTFS reading second envelope is expanded to include each TransitStop with {@link #expandToInclude(double, double)}.
 * TransitStop modes are added to transitModes variables with help of {@link #addMode(TraverseMode)}
 */
@XmlRootElement
public class GraphMetadata implements Serializable {

    /** The bounding box of the graph, in decimal degrees. */
    private double lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude;

    private HashSet<TraverseMode> transitModes = new HashSet<TraverseMode>();

    private double centerLatitude;

    private double centerLongitude;

    /**
     * This is envelope with all of OSM data and after {@link #updateEnvelope()} is called with all of data.
     */
    private WorldEnvelope envelope;

    /**
     * This envelope starts with from envelope but gets expanded with {@link #expandToInclude(double, double)} for each {@link org.opentripplanner.routing.vertextype.TransitStop}.
     *
     * In {@link #updateEnvelope()} replaces envelope.
     */
    private transient WorldEnvelope newEnvelope;

    public GraphMetadata() {
    	// 0-arg constructor avoids com.sun.xml.bind.v2.runtime.IllegalAnnotationsException
    }

    public GraphMetadata(Graph graph) {
        /* generate extents */
        envelope = new WorldEnvelope();

        for (Vertex v : graph.getVertices()) {
            Coordinate c = v.getCoordinate();
            envelope.expandToInclude(c);
        }

        setLowerLeftLongitude(envelope.getLowerLeftLongitude());
        setUpperRightLongitude(envelope.getUpperRightLongitude());
        setLowerLeftLatitude(envelope.getLowerLeftLatitude());
        setUpperRightLatitude(envelope.getUpperRightLatitude());

        newEnvelope = new WorldEnvelope(envelope);

        Optional<Coordinate> centerOptional = graph.getCenter();
        addCenter(centerOptional);

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

    @XmlElement
    public HashSet<TraverseMode> getTransitModes() {
        return transitModes;
    }

    public void setTransitModes(HashSet<TraverseMode> transitModes) {
        this.transitModes = transitModes;
    }

    public double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    /**
     * @return true if coordinate is contained in graph envelope
     */
    public boolean contains(Coordinate c) {
        return envelope.contains(c);
    }

    /**
     * Expands new envelope to include given point
     *
     * This doesn't change envelope used in {@link #contains(Coordinate)}
     * Because envelope needs to be unchanges because it is used in linking in
     * {@link org.opentripplanner.graph_builder.module.StreetLinkerModule} to see
     * if stops are inside OSM data area.
     *
     * @param  x  the value to lower the minimum x to or to raise the maximum x to
     * @param  y  the value to lower the minimum y to or to raise the maximum y to
     */
    public void expandToInclude(double x, double y) {
        newEnvelope.expandToInclude(x, y);
    }

    /**
     * This switches previous envelope (created from OSM data) with new one
     * which has data from TransitStops and modes
     */
    public void updateEnvelope() {
        envelope = newEnvelope;

        setLowerLeftLongitude(envelope.getLowerLeftLongitude());
        setUpperRightLongitude(envelope.getUpperRightLongitude());
        setLowerLeftLatitude(envelope.getLowerLeftLatitude());
        setUpperRightLatitude(envelope.getUpperRightLatitude());
    }

    /**
     * Adds mode of transport to transit modes in graph
     * @param mode
     */
    public void addMode(TraverseMode mode) {
        transitModes.add(mode);
    }

    /**
     * Set center coordinate from transit center in {@link Graph#calculateTransitCenter()} if transit is used
     * or as mean coordinate if not
     *
     * It is first called when OSM is loaded. Then after transit data is loaded.
     * So that center is set in all combinations of street and transit loading.
     * @param center
     */
    public void addCenter(Optional<Coordinate> center) {
        //Transit data was loaded and center was calculated with calculateTransitCenter
        if(center.isPresent()) {
            setCenterLongitude(center.get().x);
            setCenterLatitude(center.get().y);
            LOG.info("center from transit calculation");
        } else {
            // Does not work around 180th parallel.
            setCenterLatitude((upperRightLatitude + lowerLeftLatitude) / 2);
            setCenterLongitude((upperRightLongitude + lowerLeftLongitude) / 2);
            LOG.info("center from median");
        }
    }

    /**
     * This class calculates borders of envelopes that can be also on 180th meridian
     * The same way as it was previously calculated in GraphMetadata constructor
     *
     */
    class WorldEnvelope implements Serializable {

        Envelope leftEnv;
        Envelope rightEnv;

        double aRightCoordinate;

        private double lowerLeftLongitude;
        private double lowerLeftLatitude;
        private double upperRightLongitude;
        private double upperRightLatitude;

        boolean coordinatesCalculated = false;

        public WorldEnvelope() {
            this.leftEnv = new Envelope();
            this.rightEnv = new Envelope();
            this.aRightCoordinate = 0;
        }

        public WorldEnvelope(WorldEnvelope envelope) {
            this.leftEnv = envelope.leftEnv;
            this.rightEnv = envelope.rightEnv;
            this.aRightCoordinate = envelope.aRightCoordinate;
            this.coordinatesCalculated = false;
        }

        public void expandToInclude(Coordinate c) {
            this.expandToInclude(c.x, c.y);
        }

        public void expandToInclude(double x, double y) {
            if (x < 0) {
                leftEnv.expandToInclude(x, y);
            } else {
                rightEnv.expandToInclude(x, y);
                aRightCoordinate = x;
            }
        }

        /**
         * Calculates lower/upper right/left latitude and longitude of all the coordintes
         *
         * This takes into account that envelope can extends over 180th meridian
         */
        private void calculateCoordinates() {
            if (coordinatesCalculated) {
                return;
            }

            if (this.leftEnv.getArea() == 0) {
                //the entire area is in the eastern hemisphere
                this.lowerLeftLongitude = rightEnv.getMinX();
                this.upperRightLongitude = rightEnv.getMaxX();
                this.lowerLeftLatitude = rightEnv.getMinY();
                this.upperRightLatitude = rightEnv.getMaxY();
            } else if (this.rightEnv.getArea() == 0) {
                //the entire area is in the western hemisphere
                this.lowerLeftLongitude = leftEnv.getMinX();
                this.upperRightLongitude = leftEnv.getMaxX();
                this.lowerLeftLatitude = leftEnv.getMinY();
                this.upperRightLatitude = leftEnv.getMaxY();
            } else {
                //the area spans two hemispheres.  Either it crosses the prime meridian,
                //or it crosses the 180th meridian (roughly, the international date line).  We'll check a random
                //coordinate to find out

                if (aRightCoordinate < 90) {
                    //assume prime meridian
                    this.lowerLeftLongitude = leftEnv.getMinX();
                    this.upperRightLongitude = rightEnv.getMaxX();
                } else {
                    //assume 180th meridian
                    this.lowerLeftLongitude = leftEnv.getMaxX();
                    this.upperRightLongitude = rightEnv.getMinX();
                }
                this.upperRightLatitude = Math.max(rightEnv.getMaxY(), leftEnv.getMaxY());
                this.lowerLeftLatitude = Math.min(rightEnv.getMinY(), leftEnv.getMinY());
            }
            coordinatesCalculated = true;
        }

        public double getLowerLeftLongitude() {
            calculateCoordinates();
            return lowerLeftLongitude;
        }

        public double getLowerLeftLatitude() {
            calculateCoordinates();
            return lowerLeftLatitude;
        }

        public double getUpperRightLongitude() {
            calculateCoordinates();
            return upperRightLongitude;
        }

        public double getUpperRightLatitude() {
            calculateCoordinates();
            return upperRightLatitude;
        }

        public boolean contains(Coordinate c) {
            if (c.x < 0) {
                return leftEnv.contains(c);
            } else {
                return rightEnv.contains(c);
            }
        }
    }
}
