package org.opentripplanner.api.common;

import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.model.FeedScopedId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is kept so that the REST API can parse input strings and match them to a
 * GenericLocation.
 */
public class LocationMatcher {

        // Pattern for matching lat,lng strings, i.e. an optional '-' character followed by
        // one or more digits, and an optional (decimal point followed by one or more digits).
        private static final String _doublePattern = "-{0,1}\\d+(\\.\\d+){0,1}";

        // We want to ignore any number of non-digit characters at the beginning of the string, except
        // that signs are also non-digits. So ignore any number of non-(digit or sign or decimal point).
        // Regex has been rewritten following https://bugs.openjdk.java.net/browse/JDK-8189343
        // from "[^[\\d&&[-|+|.]]]*(" to "[\\D&&[^-+.]]*("
        private static final Pattern _latLonPattern = Pattern.compile("[\\D&&[^-+.]]*(" + _doublePattern
                + ")(\\s*,\\s*|\\s+)(" + _doublePattern + ")\\D*");

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
                return getGenericLocation(name, place);
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
        public static GenericLocation getGenericLocation(String label, String place) {
                if (place == null) {
                        return null;
                }

                Double lat = null;
                Double lon = null;
                FeedScopedId placeId = null;

                Matcher matcher = _latLonPattern.matcher(place);
                if (matcher.find()) {
                        lat = Double.parseDouble(matcher.group(1));
                        lon = Double.parseDouble(matcher.group(4));
                        if (FeedScopedId.isValidString(matcher.group(0))) {
                                placeId = FeedScopedId.convertFromString(matcher.group(0));
                        }
                }

                return new GenericLocation(label, placeId, lat, lon);
        }
}
