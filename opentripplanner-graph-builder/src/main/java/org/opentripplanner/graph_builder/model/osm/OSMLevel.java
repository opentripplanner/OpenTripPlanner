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
    public static final float METERS_PER_FLOOR = 3;
    public static final OSMLevel DEFAULT = fromString("", Source.NONE);
    public final int floorNumber;
    public final float altitudeMeters;
    public final String shortName;
    public final String longName;
    public final Source source;
    
    public enum Source {
        LEVEL_MAP,
        LEVEL_TAG,
        LAYER_TAG,
        UNPARSABLE,
        NONE
    }
    
    public OSMLevel(int floorNumber, float altitudeMeters, String shortName, String longName, Source source) {
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
    public static OSMLevel fromString (String spec, Source source) {

        /*  extract any altitude information after the @ character */
        float altitude = 0;
        int lastIndexAt = spec.lastIndexOf('@');
        if (lastIndexAt != -1) {
            try {
                altitude = Integer.parseInt(spec.substring(lastIndexAt + 1));
            } catch (NumberFormatException e3) {
                altitude = 0;
            }
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
                
        /* try to parse a floor number out of names and altitude */
        int floorNumber = 0;
        try {
            floorNumber = Integer.parseInt(shortName);
        } catch (NumberFormatException e) {
            try {
                floorNumber = Integer.parseInt(longName);
            } catch (NumberFormatException e2) {
                _log.warn("Could not determine ordinality of layer {}. " +
                        "Elevators will work, but costing may be incorrect. " +
                        "A level map should be used in this situation.", spec);
                floorNumber = (int)(altitude); // / METERS_PER_FLOOR);
            }
        }

        return new OSMLevel(floorNumber, altitude, shortName, longName, source);
    }
    
    public static List<OSMLevel> fromSpecList (String specList) {
        
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
            levels.add(fromString(spec, Source.LEVEL_MAP));
        }
        return levels;
    }

    public static Map<String, OSMLevel> mapFromSpecList (String specList) {
        Map<String, OSMLevel> map = new HashMap<String, OSMLevel>();
        for (OSMLevel level : fromSpecList(specList)) {
            map.put(level.shortName, level);
        }
        return map;
    }

    @Override
    public int compareTo(OSMLevel other) {
        return this.floorNumber - other.floorNumber;
    }

}
