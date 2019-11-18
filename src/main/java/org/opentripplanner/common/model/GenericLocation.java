package org.opentripplanner.common.model;

import com.google.common.base.Joiner;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.model.FeedScopedId;

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
     * The name of the place, if provided. This is pass-through information and does not affect
     * routing in any way.
     */
    public final String label;

    /**
     * Refers to a specific element in the OTP model. This can currently be a Stop or StopCollection.
     */
    public final FeedScopedId placeId;

    /**
     * Coordinates of the location. These can be used by themselves or as a fallback if placeId is
     * not found.
     */
    public final Double lat;

    public final Double lng;

    public GenericLocation(String label, FeedScopedId placeId, Double lat, Double lng) {
        this.label = label;
        this.placeId = placeId;
        this.lat = lat;
        this.lng = lng;
    }


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

    // Pattern for matching lat,lng strings, i.e. an optional '-' character followed by 
    // one or more digits, and an optional (decimal point followed by one or more digits).
    private static final String _doublePattern = "-{0,1}\\d+(\\.\\d+){0,1}";

    // We want to ignore any number of non-digit characters at the beginning of the string, except
    // that signs are also non-digits. So ignore any number of non-(digit or sign or decimal point).
    // Regex has been rewritten following https://bugs.openjdk.java.net/browse/JDK-8189343
    // from "[^[\\d&&[-|+|.]]]*(" to "[\\D&&[^-+.]]*("
    private static final Pattern _latLonPattern = Pattern.compile("[\\D&&[^-+.]]*(" + _doublePattern
            + ")(\\s*,\\s*|\\s+)(" + _doublePattern + ")\\D*");
    
    private static final Pattern _headingPattern = Pattern.compile("\\D*heading=("
            + _doublePattern + ")\\D*");

    /**
     * Constructs an empty GenericLocation.
     */
    public GenericLocation() {
        this.label = "";
        this.place = "";
    }

    /**
     * Constructs a GenericLocation with coordinates only.
     */
    public GenericLocation(double lat, double lng) {
        this.label = "";
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
        this.label = "";
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
    public GenericLocation(String label, String place) {
        this.label = label;
        this.place = place;

        if (place == null) {
            return;
        }

        Matcher matcher = _latLonPattern.matcher(place);
        if (matcher.find()) {
            this.lat = Double.parseDouble(matcher.group(1));
            this.lng = Double.parseDouble(matcher.group(4));
        }

        matcher = _headingPattern.matcher(place);
        if (matcher.find()) {
            this.heading = Double.parseDouble(matcher.group(1));
        }

    }

    public GenericLocation(String label, String vertexId, Double lat, Double lng) {
        this.label = label;
        // TODO OTP2 - this.vertexId = vertexId;
        this.lat = lat;
        this.lng = lng;
        this.place = Joiner.on(",").skipNulls().join(vertexId, lat, lng);
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
        return label != null && !label.isEmpty();
    }

    /** Returns true if this.place is set. */
    public boolean hasPlace() {
        return place != null && !place.isEmpty();
    }

    /**
     * Returns true if getCoordinate() will not return null.
     * @return
     */
    public boolean hasCoordinate() {
        return this.lat != null && this.lng != null;
    }

    public NamedPlace getNamedPlace() {
        return new NamedPlace(this.label, this.place);
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
}
