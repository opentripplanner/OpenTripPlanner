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

package org.opentripplanner.graph_builder.model.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSMLevel implements Comparable<OSMLevel> {
    
    private static Logger _log = LoggerFactory.getLogger(OSMLevel.class);

    public static final Pattern RANGE_PATTERN = Pattern.compile("^[0-9]+\\-[0-9]+$");
    public static final double METERS_PER_FLOOR = 3;
    public static final OSMLevel DEFAULT = fromString("", Source.NONE, false);
    public final int floorNumber; // 0-based
    public final double altitudeMeters;
    public final String shortName; // localized (potentially 1-based)
    public final String longName; // localized (potentially 1-based)
    public final Source source;
    
    public enum Source {
        LEVEL_MAP,
        LEVEL_TAG,
        LAYER_TAG,
        UNPARSABLE,
        NONE
    }
    
    public OSMLevel(int floorNumber, double altitudeMeters, String shortName, String longName, Source source) {
        this.floorNumber = floorNumber;
        this.altitudeMeters = altitudeMeters;
        this.shortName = shortName;
        this.longName = longName;
        this.source = source;
    }
    
    /** 
     * makes an OSMLevel from one of the semicolon-separated fields in an OSM
     * level map relation's levels= tag.
     */
    public static OSMLevel fromString (String spec, Source source, boolean incrementNonNegative) {

        /*  extract any altitude information after the @ character */
        Double altitude = null;
        int lastIndexAt = spec.lastIndexOf('@');
        if (lastIndexAt != -1) {
            try {
                altitude = Double.parseDouble(spec.substring(lastIndexAt + 1));
            } catch (NumberFormatException e) {}
            spec = spec.substring(0, lastIndexAt);
        }

        /* get short and long level names by splitting on = character */
        String shortName = ""; 
        String longName  = ""; 
        Integer indexEquals = spec.indexOf('=');
        if (indexEquals >= 1) {
            shortName = spec.substring(0, indexEquals);
            longName  = spec.substring(indexEquals + 1);
        } else {
            // set them both the same, the trailing @altitude has already been discarded
            shortName = longName = spec;
        }
                
        /* try to parse a floor number out of names */
        Integer floorNumber = null;
        try {
            floorNumber = Integer.parseInt(longName);
            if (incrementNonNegative) {
                if (source == Source.LEVEL_MAP) {
                    if (floorNumber >= 1)
                        floorNumber -= 1; // level maps are localized, floor numbers are 0-based
                } else {
                    if (floorNumber >= 0)
                        longName = Integer.toString(floorNumber + 1); // level and layer tags are 0-based
                }
            }
        } catch (NumberFormatException e) {}          
        try { 
            // short name takes precedence over long name for floor numbering
            floorNumber = Integer.parseInt(shortName);
            if (incrementNonNegative) {
                if (source == Source.LEVEL_MAP) {
                    if (floorNumber >= 1)
                        floorNumber -= 1; // level maps are localized, floor numbers are 0-based
                } else {
                    if (floorNumber >= 0)
                        shortName = Integer.toString(floorNumber + 1); // level and layer tags are 0-based
                }
            }
        } catch (NumberFormatException e) {}

        /* fall back on altitude when necessary */ 
        if (floorNumber == null && altitude != null) {
            floorNumber = (int)(altitude / METERS_PER_FLOOR);
            _log.warn("Could not determine floor number for layer {}. Guessed {} (0-based) from altitude.", spec, floorNumber);
        } 
        
        /* set default values when parsing failed */
        if (altitude == null) {
            altitude = 0.0;
        }
        if (floorNumber == null) {
            floorNumber = 0;
            _log.warn("Could not infer floor number for layer '{}'. Vertical movement will still be possible, but elevator cost might be incorrect. Consider an OSM level map.", spec);
        }
        return new OSMLevel(floorNumber, altitude, shortName, longName, source);
    }
    
    public static List<OSMLevel> fromSpecList (String specList, Source source, boolean incrementNonNegative) {
        
        List<String> levelSpecs = new ArrayList<String>();
        
        Matcher m;
        for (String level : specList.split(";")) {
            m = RANGE_PATTERN.matcher(level);
            if (m.matches()) {  // this field specifies a range of levels
                String[] range = level.split("-");
                int endOfRange = Integer.parseInt(range[1]);
                for (int i = Integer.parseInt(range[0]); i <= endOfRange; i++) {
                    levelSpecs.add(Integer.toString(i));
                }
            } else {  // this field is not a range, just a single level
                levelSpecs.add(level);
            }
        }
    
        /* build an OSMLevel for each level spec in the list */
        List<OSMLevel> levels = new ArrayList<OSMLevel>();
        for (String spec : levelSpecs) {
            levels.add(fromString(spec, source, incrementNonNegative));
        }
        return levels;
    }

    public static Map<String, OSMLevel> mapFromSpecList (String specList, Source source, boolean incrementNonNegative) {
        Map<String, OSMLevel> map = new HashMap<String, OSMLevel>();
        for (OSMLevel level : fromSpecList(specList, source, incrementNonNegative)) {
            map.put(level.shortName, level);
        }
        return map;
    }

    @Override
    public int compareTo(OSMLevel other) {
        return this.floorNumber - other.floorNumber;
    }

}
