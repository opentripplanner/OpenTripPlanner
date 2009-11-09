package org.opentripplanner.util;

/**
 * Constants
 * 
 * @author Frank Purcell
 * @version $Revision: 1.0 $
 * @since 1.0
 */
public interface Constants {

    // geo json stuff
    public static final String GEO_JSON = "{\"type\": \"Point\", \"coordinates\": [";
    public static final String GEO_JSON_TAIL = "]}";

    // PostGIS POINT(x, y) construct
    public static final String POINT_PREFIX = "POINT(";
    public static final int POINT_PREFIX_LEN = POINT_PREFIX.length();
    public static final String POINT_SUFFIX = ")";
    public static final int POINT_SUFFIX_LEN = POINT_SUFFIX.length();
    public static final String POINT_SEPARATOR = " ";
}
