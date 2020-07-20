package org.opentripplanner.util.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

/**
 * A list of coordinates encoded as a string.
 * <p>
 * See <a href="http://code.google.com/apis/maps/documentation/polylinealgorithm.html">Encoded
 * polyline algorithm format</a>
 */

public class EncodedPolylineBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String points;

    private final String levels;

    private final int length;


    @JsonCreator
    public EncodedPolylineBean(@JsonProperty("points") String points, @JsonProperty("levels") String levels, @JsonProperty("length") int length) {
        this.points = points;
        this.levels = levels;
        this.length = length;
    }

    /**
     * The encoded points of the polyline.
     */
    public String getPoints() {
        return points;
    }

    /**
     * Levels describes which points should be shown at various zoom levels. Presently, we show all
     * points at all zoom levels.
     */
    public String getLevels() {
        return levels;
    }

    public String getLevels(int defaultLevel) {
        if (levels == null) {
            StringBuilder b = new StringBuilder();
            String l = encodeNumber(defaultLevel);
            for (int i = 0; i < length; i++)
                b.append(l);
            return b.toString();
        }
        return levels;
    }

    /**
     * The number of points in the string
     */
    public int getLength() {
        return length;
    }

    private static String encodeNumber(int num) {

        StringBuffer encodeString = new StringBuffer();

        while (num >= 0x20) {
            int nextValue = (0x20 | (num & 0x1f)) + 63;
            encodeString.append((char) (nextValue));
            num >>= 5;
        }

        num += 63;
        encodeString.append((char) (num));

        return encodeString.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncodedPolylineBean that = (EncodedPolylineBean) o;
        return length == that.length &&
                Objects.equals(points, that.points) &&
                Objects.equals(levels, that.levels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(points, levels, length);
    }
}