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

package org.opentripplanner.routing.edgetype;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.common.geometry.CompactLineString;

/**
 * This represents train/tram/subway edges
 *
 * They are used when linking transit stations because level and type of transit can be used.
 * They are then removed from graph
 * Created by mabu on 18.3.2015.
 */
public class PublicTransitEdge extends Edge {

    private static Logger LOG = LoggerFactory.getLogger(PublicTransitEdge.class);

    private static final long serialVersionUID = 1L;

    private String name;

    private long osmID;

    private int[] compactGeometry;

    private boolean isBack;

    /**
     * The angle at the start of the edge geometry.
     * Internal representation is -180 to +179 integer degrees mapped to -128 to +127 (brads)
     */
    private byte inAngle;

    /** The angle at the start of the edge geometry. Internal representation like that of inAngle. */
    private byte outAngle;

    /**
     * Length is stored internally as 32-bit fixed-point (millimeters). This allows edges of up to ~2100km.
     * Distances used in calculations and exposed outside this class are still in double-precision floating point meters.
     * Someday we might want to convert everything to fixed point representations.
     */
    private int length_mm;



    private TraverseMode publicTransitType;

    private int floorNumber;
    private boolean reliableLevel;
    private boolean hasLevel = false;

    public PublicTransitEdge(int id, long osmId, StreetVertex v1, StreetVertex v2, LineString geometry,
                             String name, double length, TraverseMode publicTransitType,
                             boolean back) {
        super(id, osmId, v1, v2, back);
        this.setBack(back);
        this.setGeometry(geometry);
        this.length_mm = (int) (length * 1000); // CONVERT FROM FLOAT METERS TO FIXED MILLIMETERS
        this.name = name;
        this.setPublicTransitType(publicTransitType);
        if (geometry != null) {
            try {
                for (Coordinate c : geometry.getCoordinates()) {
                    if (Double.isNaN(c.x)) {
                        System.out.println("X DOOM");
                    }
                    if (Double.isNaN(c.y)) {
                        System.out.println("Y DOOM");
                    }
                }
                // Conversion from radians to internal representation as a single signed byte.
                // We also reorient the angles since OTP seems to use South as a reference
                // while the azimuth functions use North.
                // FIXME Use only North as a reference, not a mix of North and South!
                // Range restriction happens automatically due to Java signed overflow behavior.
                // 180 degrees exists as a negative rather than a positive due to the integer range.
                double angleRadians = DirectionUtils.getLastAngle(geometry);
                outAngle = (byte) Math.round(angleRadians * 128 / Math.PI + 128);
                angleRadians = DirectionUtils.getFirstAngle(geometry);
                inAngle = (byte) Math.round(angleRadians * 128 / Math.PI + 128);
            } catch (IllegalArgumentException iae) {
                LOG.error("exception while determining street edge angles. setting to zero. there is probably something wrong with this street segment's geometry.");
                inAngle = 0;
                outAngle = 0;
            }
        }
    }


    @Override
    public State traverse(State s0) {
        //That must be null since we can't drive on rails/tram/subways
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    	public void setName(String name) {
		this.name = name;
	}

	public LineString getGeometry() {
		return CompactLineString.uncompactLineString(fromv.getLon(), fromv.getLat(), tov.getLon(), tov.getLat(), compactGeometry, isBack());
	}

	private void setGeometry(LineString geometry) {
		this.compactGeometry = CompactLineString.compactLineString(fromv.getLon(), fromv.getLat(), tov.getLon(), tov.getLat(), isBack() ? (LineString)geometry.reverse() : geometry, isBack());
	}

	public void shareData(PublicTransitEdge reversedEdge) {
	    if (Arrays.equals(compactGeometry, reversedEdge.compactGeometry)) {
	        compactGeometry = reversedEdge.compactGeometry;
	    } else {
	        LOG.warn("Can't share geometry between {} and {}", this, reversedEdge);
	    }
	}

    public long getOsmID() {
        return osmID;
    }

    public void setOsmID(long osmID) {
        this.osmID = osmID;
    }

    @Override
    public double getDistance() {
        return length_mm / 1000.0; // CONVERT FROM FIXED MILLIMETERS TO FLOAT METERS
    }

    public boolean isBack() {
        return isBack;
    }

    public void setBack(boolean isBack) {
        this.isBack = isBack;
    }

    public TraverseMode getPublicTransitType() {
        return publicTransitType;
    }

    public void setPublicTransitType(TraverseMode publicTransitType) {
        this.publicTransitType = publicTransitType;
    }

    /**
     * Return the azimuth of the first segment in this edge in integer degrees clockwise from South.
     * TODO change everything to clockwise from North
     */
    public int getInAngle() {
        return this.inAngle * 180 / 128;
    }

    /** Return the azimuth of the last segment in this edge in integer degrees clockwise from South. */
    public int getOutAngle() {
        return this.outAngle * 180 / 128;
    }

    public void setLevel(OSMLevel level) {
        floorNumber = level.floorNumber;
        reliableLevel = level.reliable;
        hasLevel = true;
    }

    public String getNiceLevel() {
        if (!hasLevel) {
            return "";
        } else {
            return Integer.toString(floorNumber) + " [" + Boolean.toString(reliableLevel) + "]";
        }

    }

    public Integer getLevel() {
        return floorNumber;
    }

    public Boolean isReliableLevel() {
        return reliableLevel;
    }

    @Override
    public String toString() {
        return "PublicTransitEdge{" +
                "name='" + name + '\'' +
                ", publicTransitType=" + publicTransitType +
                ", level='" + getNiceLevel() + '\'' +
                '}';
    }
}
