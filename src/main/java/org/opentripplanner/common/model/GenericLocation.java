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

package org.opentripplanner.common.model;

import com.google.common.base.Joiner;
import com.vividsolutions.jts.geom.Coordinate;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class describing a location provided by clients of routing. Used to describe end points
 * (origin, destination) of a routing request as well as any intermediate points that should
 * be passed through.
 * <p>
 * Handles parsing of geospatial information from strings so that it need not be littered through
 * the routing code.
 *
 * @author avi
 */
public class GenericLocation implements Cloneable, Serializable {

    /**
     * Pattern for matching decimal numbers like lat,lng strings. It matches an optional
     * '-' character followed by one or more digits, and an optional decimal point followed
     * by one or more digits. Capturing group.
     */
    private final static String DECIMAL_PTN = "(-?\\d+(?:\\.\\d+)?)";

    /**
     * Pattern for matching an optional comma with optional whitespace before and after the
     * comma. Non-capturing group.
     */
    private final static String SEPARATOR_PTN = "(?:\\s+|\\s*,\\s*)";

    /**
     * Pattern for matching coordinates: lat, lon
     * We want to ignore any whitespace and comma at the beginning of the string,
     * because the coordinates may follow a vertexId and we must make sure the coordinates
     * are at the end; hence not matching the last part of a vertexId (it it i a number).
     * Trailing whitespace is ignored.
     */
    private final static Pattern COORDINATES_PATTERN = Pattern
            .compile(SEPARATOR_PTN + "?" + DECIMAL_PTN + SEPARATOR_PTN + DECIMAL_PTN + "\\s*$");

    /**
     * Pattern for matching the optional heading parameter
     */
    private final static Pattern HEADING_PATTERN = Pattern.compile("heading=(" + DECIMAL_PTN + ")");

    /**
     * Pattern for matching the optional edgeId parameter
     */
    private final static Pattern EDGE_ID_PATTERN = Pattern.compile("edgeId=(\\d+)");

    /**
     * Pattern for matching the optional vertexId as part of the 'name' part. The OTP debuger GUI
     * formats request like this: "PLACE_NAME (VERTEX_ID)::LOCATION". So, if we encounter something
     * in parenthesises we treat it as an VertexId.
     */
    private final static Pattern VERTEX_ID_PATTERN = Pattern.compile("\\(([\\w-:]+)\\)$");

    /**
     * The name of the place, if provided.
     */
    public final String name;

    /**
     * The identifier of the place, if provided. May be vertex ID, a lat,lng string or both.
     * If both vertexId and coordinates is passed in the vertexId is used if it exist, if not
     * the coordinates are used.
     */
    public final String place;

    /**
     * The vertex ID for the place  given.
     */
    public String vertexId;

    /**
     * The ID of the edge this location is on if any.
     */
    public Integer edgeId;

    /**
     * Coordinates of the place, if provided.
     */
    public Double lat;

    public Double lng;

    /**
     * Observed heading if any.
     *
     * Direction of travel in decimal degrees from -180° to +180° relative to
     * true north.
     *
     * 0      = heading true north.
     * +/-180 = heading south.
     */
    public Double heading;

    /**
     * Constructs an empty GenericLocation.
     */
    public GenericLocation() {
        this.name = "";
        this.place = "";
    }

    /**
     * Constructs a GenericLocation with coordinates only.
     */
    public GenericLocation(double lat, double lng) {
        this.name = "";
        this.place = "";
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * Constructs a GenericLocation with coordinates only.
     */
    public GenericLocation(Coordinate coord) {
        this(coord.y, coord.x);
    }

    /**
     * Constructs a GenericLocation with coordinates and heading.
     */
    public GenericLocation(double lat, double lng, double heading) {
        this.name = "";
        this.place = "";
        this.lat = lat;
        this.lng = lng;
        this.heading = heading;
    }



    /**
     * Construct from a name, place pair.
     * Parses latitude, longitude data, heading and numeric edge ID out of the place string.
     * Note that if the place string does not appear to contain a lat/lon pair, heading, or edge ID
     * the GenericLocation will be missing that information but will still retain the place string,
     * which will be interpreted during routing context construction as a vertex label within the
     * graph for the appropriate routerId (by StreetVertexIndexServiceImpl.getVertexForLocation()).
     * TODO: Perhaps the interpretation as a vertex label should be done here for clarity.
     */
    public GenericLocation(String name, String place) {
        this.name = name;
        this.place = place;

        if (place == null) {
            return;
        }

        String text = place;

        Matcher m = HEADING_PATTERN.matcher(text);
        if (m.find()) {
            heading = Double.parseDouble(m.group(1));
            text = removeMatchFromString(m, text);
        }

        m = EDGE_ID_PATTERN.matcher(text);
        if (m.find()) {
            edgeId = Integer.parseInt(m.group(1));
            text = removeMatchFromString(m, text);
        }

        m = COORDINATES_PATTERN.matcher(text);
        if (m.find()) {
            this.lat = Double.parseDouble(m.group(1));
            this.lng = Double.parseDouble(m.group(2));
            text = removeMatchFromString(m, text);
        }

        if(name != null) {
            m = VERTEX_ID_PATTERN.matcher(name);

            if(m.find()) {
                vertexId = m.group(1);
            }
        }
        if(vertexId == null && !text.isEmpty()) {
            vertexId = text;
        }
    }

    public GenericLocation(String name, String vertexId, Double lat, Double lng) {
        this.name = name;
        this.vertexId = vertexId;
        this.lat = lat;
        this.lng = lng;
        this.place = Joiner.on(",").skipNulls().join(vertexId, lat, lng);
    }

    /**
     * Same as above, but draws name and place string from a NamedPlace object.
     *
     * @param np
     */
    public GenericLocation(NamedPlace np) {
        this(np.name, np.place);
    }

    /**
     * Creates the GenericLocation by parsing a "name::place" string, where "place" is a latitude,longitude string or a vertex ID.
     *
     * @param input
     * @return
     */
    public static GenericLocation fromOldStyleString(String input) {
        String name = "";
        String place = input;
        if (input.contains("::")) {
            String[] parts = input.split("::", 2);
            name = parts[0];
            place = parts[1];
        }
        return new GenericLocation(name, place);
    }

    /**
     * Returns true if this.heading is not null.
     * @return
     */
    public boolean hasHeading() {
        return heading != null;
    }

    /** Returns true if this.name is set. */
    public boolean hasName() {
        return name != null && !name.isEmpty();
    }

    /** Returns true if this.place is set. */
    public boolean hasPlace() {
        return place != null && !place.isEmpty();
    }

    /** Returns true if vertexId is set. */
    public boolean hasVertexId() {
        return vertexId != null;
    }

    /**
     * Returns true if getCoordinate() will not return null.
     * @return
     */
    public boolean hasCoordinate() {
        return this.lat != null && this.lng != null;
    }

    /**
     * Returns true if getEdgeId would not return null.
     * @return
     */
    public boolean hasEdgeId() {
        return this.edgeId != null;
    }

    public NamedPlace getNamedPlace() {
        return new NamedPlace(this.name, this.place);
    }

    /**
     * Returns this as a Coordinate object.
     * @return
     */
    public Coordinate getCoordinate() {
        if (this.lat == null || this.lng == null) {
            return null;
        }
        return new Coordinate(this.lng, this.lat);
    }

    /**
     * Represents the location as an old-style string for clients that relied on that behavior.
     *
     * TODO(flamholz): clients should stop relying on these being strings and then we can return a string here that fully represents the contents of
     * the object.
     */
    @Override
    public String toString() {
        if (this.place != null && !this.place.isEmpty()) {
            if (this.name == null || this.name.isEmpty()) {
                return this.place;
            } else {
                return String.format("%s::%s", this.name, this.place);
            }
        }

        return String.format("%s,%s", this.lat, this.lng);
    }

    /**
     * Returns a descriptive string that has the information that I wish toString() returned.
     */
    public String toDescriptiveString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<GenericLocation lat,lng=").append(this.lat).append(",").append(this.lng);
        if (this.hasHeading()) {
            sb.append(" heading=").append(this.heading);
        }
        if (this.hasEdgeId()) {
            sb.append(" edgeId=").append(this.edgeId);
        }
        sb.append(">");
        return sb.toString();
    }

    @Override
    public GenericLocation clone() {
        try {
            return (GenericLocation) super.clone();
        } catch (CloneNotSupportedException e) {
            /* this will never happen since our super is the cloneable object */
            throw new RuntimeException(e);
        }
    }

    static String removeMatchFromString(Matcher m, String text) {
        int index0  = m.start();
        int index1 = m.end();

        if(index0 == 0) {
            return text.substring(index1).trim();
        }
        else if(index1 == text.length()) {
            return text.substring(0, index0).trim();
        }
        else {
            return (text.substring(0, index0) + text.substring(index1)).trim();
        }
    }
}
