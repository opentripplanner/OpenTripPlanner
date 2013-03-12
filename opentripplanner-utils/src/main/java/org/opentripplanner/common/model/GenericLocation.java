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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vividsolutions.jts.geom.Coordinate;

import lombok.Data;

/**
 * Class describing a location provided by clients of routing. Used to describe end points (origin, destination) of a routing request as well as any
 * intermediate points that should be passed through.
 * 
 * Handles parsing of geospatial information from strings so that it need not be littered through the routing code.
 * 
 * @author avi
 */
@Data
public class GenericLocation {

    /**
     * The name of the place, if provided.
     */
    private final String name;

    /**
     * The identifier of the place, if provided. May be a lat,lng string or a vertex ID.
     */
    private final String place;

    /**
     * Coordinates of the place, if provided.
     */
    private Double lat;

    private Double lng;
    
    /**
     * Observed heading if any.
     * 
     * Direction of travel in decimal degrees from -180° to +180° relative to
     * true north.
     * 
     * 0      = heading true north.
     * +/-180 = heading south.
     */
    private Double heading;

    // Pattern for parsing lat,lng strings.
    private static final String _doublePattern = "-{0,1}\\d+(\\.\\d+){0,1}";

    private static final Pattern _latLonPattern = Pattern.compile("^\\s*(" + _doublePattern
            + ")(\\s*,\\s*|\\s+)(" + _doublePattern + ")\\s*$");

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
     * 
     * Parses latitude, longitude data from the place string.
     * 
     * @param name
     * @param place
     */
    public GenericLocation(String name, String place) {
        this.name = name;
        this.place = place;

        if (place == null) {
            return;
        }
        
        Matcher matcher = _latLonPattern.matcher(place);
        if (matcher.matches()) {
            this.lat = Double.parseDouble(matcher.group(1));
            this.lng = Double.parseDouble(matcher.group(4));
        }
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
            String[] parts = input.split("::");
            name = parts[0];
            place = parts[1];
        }
        return new GenericLocation(name, place);
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
}
