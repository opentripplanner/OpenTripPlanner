/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.impl.osm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.patch.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.Data;

@Data
public class WayPropertySet {
    private static Logger _log = LoggerFactory.getLogger(WayPropertySet.class);

    private List<WayPropertyPicker> wayProperties;

    private List<CreativeNamerPicker> creativeNamers;

    private List<SlopeOverridePicker> slopeOverrides;
    
    /**
     * SpeedPickers for automobile speeds.
     */
    private List<SpeedPicker> speedPickers;
    
    /**
     * The speed for street segments that match no speedPicker.    
     */
    private Float defaultSpeed;

    private List<NotePicker> notes;
    
    private Pattern maxSpeedPattern;
    
    public WayProperties defaultProperties;

    public WayPropertySet() {
        /* sensible defaults */
        defaultProperties = new WayProperties();
        defaultProperties.setSafetyFeatures(new P2<Double>(1.0, 1.0));
        defaultProperties.setPermission(StreetTraversalPermission.ALL);
        defaultSpeed = 11.2f; // 11.2 m/s, ~25 mph, standard speed limit in the US 
        wayProperties = new ArrayList<WayPropertyPicker>();
        creativeNamers = new ArrayList<CreativeNamerPicker>();
        slopeOverrides = new ArrayList<SlopeOverridePicker>();
        speedPickers = new ArrayList<SpeedPicker>();
        notes = new ArrayList<NotePicker>();
        // regex courtesy http://wiki.openstreetmap.org/wiki/Key:maxspeed
        // and edited
        maxSpeedPattern = Pattern.compile("^([0-9][\\.0-9]+?)(?:[ ]?(kmh|km/h|kmph|kph|mph|knots))?$");
    }

    public WayProperties getDataForWay(OSMWithTags way) {
        WayProperties leftResult = defaultProperties;
        WayProperties rightResult = defaultProperties;
        int bestLeftScore = 0;
        int bestRightScore = 0;
        List<WayProperties> leftMixins = new ArrayList<WayProperties>();
        List<WayProperties> rightMixins = new ArrayList<WayProperties>();
        for (WayPropertyPicker picker : getWayProperties()) {
            OSMSpecifier specifier = picker.getSpecifier();
            WayProperties wayProperties = picker.getProperties();
            P2<Integer> score = specifier.matchScores(way);
            int leftScore = score.getFirst();
            int rightScore = score.getSecond();
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
        result.setSafetyFeatures(new P2<Double>(rightResult.getSafetyFeatures().getFirst(),
                leftResult.getSafetyFeatures().getSecond()));

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
            _log.debug("Used default permissions: " + all_tags);
        }
        return result;
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
        double first = safetyFeatures.getFirst();
        double second = safetyFeatures.getSecond();
        for (WayProperties properties : mixins) {
            if (right) {
                second *= properties.getSafetyFeatures().getSecond();
            } else {
                first *= properties.getSafetyFeatures().getFirst();
            }
        }
        result.setSafetyFeatures(new P2<Double>(first, second));
    }

    public String getCreativeNameForWay(OSMWithTags way) {
        CreativeNamer bestNamer = null;
        int bestScore = 0;
        for (CreativeNamerPicker picker : creativeNamers) {
            OSMSpecifier specifier = picker.getSpecifier();
            CreativeNamer namer = picker.getNamer();
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
     * Calculate the automobile speed, in meters per second, for this way.
     */
    public float getCarSpeedForWay(OSMWithTags way, boolean back) {
        // first, check for maxspeed tags
        float speed = -1;
        float currentSpeed;
        
        if (way.hasTag("maxspeed:motorcar"))
            speed = getMetersSecondFromSpeed(way.getTag("maxspeed:motorcar"));
        
        if (speed == -1 && !back && way.hasTag("maxspeed:forward"))
            speed = getMetersSecondFromSpeed(way.getTag("maxspeed:forward"));
        
        if (speed == -1 && back && way.hasTag("maxspeed:reverse"))
            speed = getMetersSecondFromSpeed(way.getTag("maxspeed:reverse")); 
            
        if (speed == -1 && way.hasTag("maxspeed:lanes")) {
            for (String lane : way.getTag("maxspeed:lanes").split("|")) {
                currentSpeed = getMetersSecondFromSpeed(lane);
                if (currentSpeed > speed)
                    speed = currentSpeed;
            }
        }
        
        if (way.hasTag("maxspeed") && speed == -1)
            speed = getMetersSecondFromSpeed(way.getTag("maxspeed"));
                    
        int bestScore = 0;
        float bestSpeed = -1;
        int score;
        
        for (SpeedPicker picker : speedPickers) {
            OSMSpecifier specifier = picker.getSpecifier();
            score = specifier.matchScore(way);
            if (score > bestScore) {
                bestScore = score;
                bestSpeed = picker.getSpeed();
            }
        }
        
        if (bestSpeed != -1)
            return bestSpeed;
        else
            return this.defaultSpeed;
    }

    public Set<Alert> getNoteForWay(OSMWithTags way) {
        HashSet<Alert> out = new HashSet<Alert>();
        for (NotePicker picker : notes) {
            OSMSpecifier specifier = picker.getSpecifier();
            NoteProperties noteProperties = picker.getNoteProperties();
            if (specifier.matchScore(way) > 0) {
                out.add(Alert.createSimpleAlerts(noteProperties.generateNote(way).intern()));
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
        getWayProperties().add(new WayPropertyPicker(spec, properties, mixin));
    }

    public void addProperties(OSMSpecifier spec, WayProperties properties) {
        getWayProperties().add(new WayPropertyPicker(spec, properties, false));
    }

    public void addCreativeNamer(OSMSpecifier spec, CreativeNamer namer) {
        getCreativeNamers().add(new CreativeNamerPicker(spec, namer));
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
    
    public float getMetersSecondFromSpeed(String speed) {
        Matcher m = maxSpeedPattern.matcher(speed);
        if (!m.matches())
            return -1;
        
        int originalUnits = Integer.parseInt(m.group(1));
        
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
            return -1;
        
        return metersSecond;
    }
}
