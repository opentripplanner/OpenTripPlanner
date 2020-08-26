package org.opentripplanner.graph_builder.module.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.sort;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.services.notes.NoteMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentripplanner.util.I18NString;

/**
 * Information given to the GraphBuilder about how to assign permissions, safety values, names, etc. to edges based on OSM tags.
 * TODO rename so that the connection with OSM tags is obvious
 *
 * WayPropertyPickers, CreativeNamePickers, SlopeOverridePickers, and SpeedPickers are applied to ways based on how well
 * their OSMSpecifiers match a given OSM way. Generally one OSMSpecifier will win out over all the others based on the
 * number of exact, partial, and wildcard tag matches. See OSMSpecifier for more details on the matching process.
 */
public class WayPropertySet {
    private static Logger LOG = LoggerFactory.getLogger(WayPropertySet.class);

    private List<WayPropertyPicker> wayProperties;

    /** Assign names to ways that do not have them based on OSM tags. */
    private List<CreativeNamerPicker> creativeNamers;

    private List<SlopeOverridePicker> slopeOverrides;
    
    /** Assign automobile speeds based on OSM tags. */
    private List<SpeedPicker> speedPickers;
    
    /** The automobile speed for street segments that do not match any SpeedPicker. */
    public Float defaultSpeed;

    private List<NotePicker> notes;
    
    private Pattern maxSpeedPattern;

    /** The WayProperties applied to all ways that do not match any WayPropertyPicker. */
    public WayProperties defaultProperties;

    public WayPropertySetSource base;

    private HashMap<String, WayProperties> wayPropertyLookup;

    private HashSet<String> possibleWayPropertyTagValues;

    private HashMap<String, Float> carSpeedLookup;

    private HashSet<String> possibleCarSpeedTagValues;

    public WayPropertySet() {
        /* sensible defaults */
        defaultProperties = new WayProperties();
        defaultProperties.setSafetyFeatures(new P2<Double>(1.0, 1.0));
        defaultProperties.setPermission(StreetTraversalPermission.ALL);
        defaultSpeed = 11.2f; // 11.2 m/s ~= 25 mph ~= 40 kph, standard speed limit in the US
        wayProperties = new ArrayList<WayPropertyPicker>();
        creativeNamers = new ArrayList<CreativeNamerPicker>();
        slopeOverrides = new ArrayList<SlopeOverridePicker>();
        speedPickers = new ArrayList<SpeedPicker>();
        notes = new ArrayList<NotePicker>();
        // regex courtesy http://wiki.openstreetmap.org/wiki/Key:maxspeed
        // and edited
        maxSpeedPattern = Pattern.compile("^([0-9][\\.0-9]+?)(?:[ ]?(kmh|km/h|kmph|kph|mph|knots))?$");
    }

    /**
     * Initializes lookups and indexes for quick lookup of applicable values for way properties and car speeds. This
     * function must be called before looking up way properties and car speeds for ways in order for those caches to be
     * properly initialized.
     */
    public void index() {
        // index for way properties
        wayPropertyLookup = new HashMap<>();
        possibleWayPropertyTagValues = new HashSet<>();
        for (WayPropertyPicker wayProperty : wayProperties) {
            for (P2<String> kvpair : wayProperty.getSpecifier().kvpairs) {
                possibleWayPropertyTagValues.add(String.format("%s=%s", kvpair.first, kvpair.second));
            }
        }

        // index for car speed lookup
        carSpeedLookup = new HashMap<>();
        possibleCarSpeedTagValues = new HashSet<>();
        for (SpeedPicker speedPicker : speedPickers) {
            for (P2<String> kvpair : speedPicker.specifier.kvpairs) {
                possibleCarSpeedTagValues.add(String.format("%s=%s", kvpair.first, kvpair.second));
            }
        }
        // also add manual search terms from getCarSpeedForWay method
        possibleCarSpeedTagValues.add("maxspeed:motorcar=*");
        possibleCarSpeedTagValues.add("maxspeed:forward=*");
        possibleCarSpeedTagValues.add("maxspeed:reverse=*");
        possibleCarSpeedTagValues.add("maxspeed:lanes=*");
        possibleCarSpeedTagValues.add("maxspeed=*");
    }

    /**
     * compute lookup key for way based on applicable tags/values
     * @param way The way from which to calculate a key
     * @param possibleTagValues The possible tag values that would apply for this way
     */
    private String getKeyFromApplicableTags(OSMWithTags way, HashSet possibleTagValues) {
        List<String> applicableTagValues = new ArrayList<>();
        Map<String, String> wayTags = way.getTags();
        if (wayTags == null) {
            // no tags added to way. Probably occurs only during testing.
            return "";
        }
        for (Entry<String, String> wayTagValue : wayTags.entrySet()) {
            String tag = wayTagValue.getKey();
            String value = wayTagValue.getValue();
            String tagValue = String.format("%s=%s", tag, value);

            // create list of tags to see if any of them are in applicable list
            List<String> possibleMatches = new ArrayList<>();

            // add exact match use case
            possibleMatches.add(tagValue);

            // add wildcard value for tag
            possibleMatches.add(String.format("%s=*", tag));

            // if value contains a colon and thus possibly a suffix (such as surface=cobblestone:flattened), use the
            // root tag value
            String rootValue = null;
            if (value.contains(":")) {
                rootValue = value.split(":", 2)[0];
                // add exact tag wth root value
                possibleMatches.add(String.format("%s=%s", tag, rootValue));
            }

            // if tag contains a colon and thus a suffix (such as cycleway:right), use the root tag key to check if
            // there is a match
            if (tag.contains(":")) {
                String rootTag = tag.split(":", 2)[0];
                // add both exact and wilcard tag/values
                possibleMatches.add(String.format("%s=%s", rootTag, value));
                possibleMatches.add(String.format("%s=*", rootTag));

                // also add rootValue if applicable
                if (rootValue != null) {
                    possibleMatches.add(String.format("%s=%s", rootTag, rootValue));
                }
            }

            for (String possibleMatch : possibleMatches) {
                if (possibleTagValues.contains(possibleMatch)) {
                    // add the exact tag value to make sure cache key is for this exact set of tag/values
                    applicableTagValues.add(tagValue);
                    // break as there is no need to search for other possibilities
                    break;
                }
            }
        }
        // sort values to make deterministic key
        sort(applicableTagValues);
        return String.join(";", applicableTagValues);
    }

    /**
     * Applies the WayProperties whose OSMPicker best matches this way. In addition, WayProperties that are mixins
     * will have their safety values applied if they match at all.
     */
    public WayProperties getDataForWay(OSMWithTags way) {
        String wayPropertiesKey = getKeyFromApplicableTags(way, possibleWayPropertyTagValues);
        // check if lookup key found
        WayProperties cachedWayProperties = wayPropertyLookup.get(wayPropertiesKey);
        if (cachedWayProperties != null) {
            return cachedWayProperties;
        }

        WayProperties leftResult = defaultProperties;
        WayProperties rightResult = defaultProperties;
        int bestLeftScore = 0;
        int bestRightScore = 0;
        List<WayProperties> leftMixins = new ArrayList<WayProperties>();
        List<WayProperties> rightMixins = new ArrayList<WayProperties>();
        for (WayPropertyPicker picker : wayProperties) {
            OSMSpecifier specifier = picker.getSpecifier();
            WayProperties wayProperties = picker.getProperties();
            P2<Integer> score = specifier.matchScores(way);
            int leftScore = score.first;
            int rightScore = score.second;
            if (picker.isSafetyMixin()) {
                if (leftScore > 0) {
                    leftMixins.add(wayProperties);
                }
                if (rightScore > 0) {
                    rightMixins.add(wayProperties);
                }
            } else {
                if (leftScore > bestLeftScore) {

                    leftResult = wayProperties;
                    bestLeftScore = leftScore;
                }
                if (rightScore > bestRightScore) {
                    rightResult = wayProperties;
                    bestRightScore = rightScore;
                }
            }
        }

        WayProperties result = rightResult.clone();
        result.setSafetyFeatures(new P2<Double>(rightResult.getSafetyFeatures().first,
                leftResult.getSafetyFeatures().second));

        /* apply mixins */
        if (leftMixins.size() > 0) {
            applyMixins(result, leftMixins, false);
        }
        if (rightMixins.size() > 0) {
            applyMixins(result, rightMixins, true);
        }
        if ((bestLeftScore == 0 || bestRightScore == 0)
                && (leftMixins.size() == 0 || rightMixins.size() == 0)) {
            String all_tags = dumpTags(way);
            LOG.debug("Used default permissions: " + all_tags);
        }
        wayPropertyLookup.put(wayPropertiesKey, result);
        return result;
    }

    /**
     * Returns the current way property lookup hashmap. Used for testing only.
     */
    public HashMap<String, WayProperties> getWayPropertyLookup () {
        return wayPropertyLookup;
    }

    private String dumpTags(OSMWithTags way) {
        /* generate warning message */
        String all_tags = null;
        Map<String, String> tags = way.getTags();
        for (Entry<String, String> entry : tags.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String tag = key + "=" + value;
            if (all_tags == null) {
                all_tags = tag;
            } else {
                all_tags += "; " + tag;
            }
        }
        return all_tags;
    }

    private void applyMixins(WayProperties result, List<WayProperties> mixins, boolean right) {
        P2<Double> safetyFeatures = result.getSafetyFeatures();
        double first = safetyFeatures.first;
        double second = safetyFeatures.second;
        for (WayProperties properties : mixins) {
            if (right) {
                second *= properties.getSafetyFeatures().second;
            } else {
                first *= properties.getSafetyFeatures().first;
            }
        }
        result.setSafetyFeatures(new P2<Double>(first, second));
    }

    public I18NString getCreativeNameForWay(OSMWithTags way) {
        CreativeNamer bestNamer = null;
        int bestScore = 0;
        for (CreativeNamerPicker picker : creativeNamers) {
            OSMSpecifier specifier = picker.specifier;
            CreativeNamer namer = picker.namer;
            int score = specifier.matchScore(way);
            if (score > bestScore) {
                bestNamer = namer;
                bestScore = score;
            }
        }
        if (bestNamer == null) {
            return null;
        }
        return bestNamer.generateCreativeName(way);
    }
    
    /**
     * Get the automobile speed, in meters per second, for this way. Try to get value from the cache, otherwise
     * calculate and store in cache.
     */
    public float getCarSpeedForWay(OSMWithTags way, boolean back) {
        String wayPropertiesKey = getKeyFromApplicableTags(way, possibleCarSpeedTagValues);
        // also append back value
        wayPropertiesKey = String.format("%s;%s=%b", wayPropertiesKey, "back=", back);
        // check if lookup key found
        Float cachedSpeed = carSpeedLookup.get(wayPropertiesKey);
        if (cachedSpeed != null) {
            return cachedSpeed;
        }

        Float speed = calculateCarSpeedForWay(way, back);
        carSpeedLookup.put(wayPropertiesKey, speed);
        return speed;
    }

    /**
     * Calculate the automobile speed, in meters per second, for this way.
     */
    private float calculateCarSpeedForWay(OSMWithTags way, boolean back) {
        // first, check for maxspeed tags
        Float speed = null;
        Float currentSpeed;
        
        if (way.hasTag("maxspeed:motorcar"))
            speed = getMetersSecondFromSpeed(way.getTag("maxspeed:motorcar"));
        
        if (speed == null && !back && way.hasTag("maxspeed:forward"))
            speed = getMetersSecondFromSpeed(way.getTag("maxspeed:forward"));
        
        if (speed == null && back && way.hasTag("maxspeed:reverse"))
            speed = getMetersSecondFromSpeed(way.getTag("maxspeed:reverse")); 
            
        if (speed == null && way.hasTag("maxspeed:lanes")) {
            for (String lane : way.getTag("maxspeed:lanes").split("\\|")) {
                currentSpeed = getMetersSecondFromSpeed(lane);
                // Pick the largest speed from the tag
                // currentSpeed might be null if it was invalid, for instance 10|fast|20
                if (currentSpeed != null && (speed == null || currentSpeed > speed))
                    speed = currentSpeed;
            }
        }
        
        if (way.hasTag("maxspeed") && speed == null)
            speed = getMetersSecondFromSpeed(way.getTag("maxspeed"));
        
        // this would be bad, as the segment could never be traversed by an automobile
        // The small epsilon is to account for possible rounding errors
        if (speed != null && speed < 0.0001)
            LOG.warn("Zero or negative automobile speed detected at {} based on OSM " +
            		"maxspeed tags; ignoring these tags", this);
        
        // if there was a defined speed and it's not 0, we're done
        if (speed != null)
            return speed;
                    
        // otherwise, we use the speedPickers
        
        int bestScore = 0;
        Float bestSpeed = null;
        int score;
        
        // SpeedPickers are constructed in DefaultWayPropertySetSource with an OSM specifier
        // (e.g. highway=motorway) and a default speed for that segment.
        for (SpeedPicker picker : speedPickers) {
            OSMSpecifier specifier = picker.specifier;
            score = specifier.matchScore(way);
            if (score > bestScore) {
                bestScore = score;
                bestSpeed = picker.speed;
            }
        }
        
        if (bestSpeed != null)
            return bestSpeed;
        else
            return this.defaultSpeed;
    }

    /**
     * Returns the current car speed lookup hashmap. Used for testing only.
     */
    public HashMap<String, Float> getCarSpeedLookup() {
        return carSpeedLookup;
    }

    public Set<T2<Alert, NoteMatcher>> getNoteForWay(OSMWithTags way) {
        HashSet<T2<Alert, NoteMatcher>> out = new HashSet<>();
        for (NotePicker picker : notes) {
            OSMSpecifier specifier = picker.specifier;
            NoteProperties noteProperties = picker.noteProperties;
            if (specifier.matchScore(way) > 0) {
                out.add(noteProperties.generateNote(way));
            }
        }
        if (out.size() == 0) {
            return null;
        }
        return out;
    }

    public boolean getSlopeOverride(OSMWithTags way) {
        boolean result = false;
        int bestScore = 0;
        for (SlopeOverridePicker picker : slopeOverrides) {
            OSMSpecifier specifier = picker.getSpecifier();
            int score = specifier.matchScore(way);
            if (score > bestScore) {
                result = picker.getOverride();
                bestScore = score;
            }
        }
        return result;
    }

    public void addProperties(OSMSpecifier spec, WayProperties properties, boolean mixin) {
        wayProperties.add(new WayPropertyPicker(spec, properties, mixin));
    }

    public void addProperties(OSMSpecifier spec, WayProperties properties) {
        wayProperties.add(new WayPropertyPicker(spec, properties, false));
    }

    public void addCreativeNamer(OSMSpecifier spec, CreativeNamer namer) {
        creativeNamers.add(new CreativeNamerPicker(spec, namer));
    }

    public void addNote(OSMSpecifier osmSpecifier, NoteProperties properties) {
        notes.add(new NotePicker(osmSpecifier, properties));
    }

    public void setSlopeOverride(OSMSpecifier spec, boolean override) {
        slopeOverrides.add(new SlopeOverridePicker(spec, override));
    }

    public boolean equals(Object o) {
        if (o instanceof WayPropertySet) {
            WayPropertySet other = (WayPropertySet) o;
            return (defaultProperties.equals(other.defaultProperties)
                    && wayProperties.equals(other.wayProperties)
                    && creativeNamers.equals(other.creativeNamers)
                    && slopeOverrides.equals(other.slopeOverrides) && notes.equals(other.notes));
        }
        return false;
    }

    public int hashCode() {
        return defaultProperties.hashCode() + wayProperties.hashCode() + creativeNamers.hashCode()
                + slopeOverrides.hashCode();
    }

    public void addSpeedPicker(SpeedPicker picker) {
        this.speedPickers.add(picker);
    }
    
    public Float getMetersSecondFromSpeed(String speed) {
        Matcher m = maxSpeedPattern.matcher(speed);
        if (!m.matches())
            return null;
        
        float originalUnits;
        try {
            originalUnits = (float) Double.parseDouble(m.group(1));
        } catch (NumberFormatException e) {
            LOG.warn("Could not parse max speed {}", m.group(1));
            return null;
        }
        
        String units = m.group(2);
        if (units == null || units.equals(""))
            units = "kmh";
        
        // we'll be doing quite a few string comparisons here
        units = units.intern();
        
        float metersSecond;
        
        if (units == "kmh" || units == "km/h" || units == "kmph" || units == "kph")
            metersSecond = 0.277778f * originalUnits;
        else if (units == "mph")
            metersSecond = 0.446944f * originalUnits;
        else if (units == "knots")
            metersSecond = 0.514444f * originalUnits;
        else
            return null;
        
        return metersSecond;
    }
    
	public void createNames(String spec, String patternKey) {
		String pattern = patternKey;
		CreativeNamer namer = new CreativeNamer(pattern);
		addCreativeNamer(new OSMSpecifier(spec), namer);
	}

	public  void createNotes(String spec, String patternKey, NoteMatcher matcher) {
		String pattern = patternKey;
		// TODO: notes aren't localized
		NoteProperties properties = new NoteProperties(pattern, matcher);
		addNote(new OSMSpecifier(spec), properties);
	}

	public void setProperties(String spec,
			StreetTraversalPermission permission) {
		setProperties( spec, permission, 1.0, 1.0);
	}

	/**
	 * Note that the safeties here will be adjusted such that the safest street
	 * has a safety value of 1, with all others scaled proportionately.
	 */
	public void setProperties(String spec,
			StreetTraversalPermission permission, double safety, double safetyBack) {
		setProperties(spec, permission, safety, safetyBack, false);
	}

	public void setProperties(String spec,
			StreetTraversalPermission permission, double safety, double safetyBack, boolean mixin) {
		WayProperties properties = new WayProperties();
		properties.setPermission(permission);
		properties.setSafetyFeatures(new P2<Double>(safety, safetyBack));
		addProperties(new OSMSpecifier(spec), properties, mixin);
	}

	public void setCarSpeed(String spec, float speed) {
		SpeedPicker picker = new SpeedPicker();
		picker.specifier = new OSMSpecifier(spec);
		picker.speed = speed;
		addSpeedPicker(picker);
	}

}
