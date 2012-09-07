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

    private List<NotePicker> notes;

    public WayProperties defaultProperties;

    public WayPropertySet() {
        /* sensible defaults */
        defaultProperties = new WayProperties();
        defaultProperties.setSafetyFeatures(new P2<Double>(1.0, 1.0));
        defaultProperties.setPermission(StreetTraversalPermission.ALL);
        wayProperties = new ArrayList<WayPropertyPicker>();
        creativeNamers = new ArrayList<CreativeNamerPicker>();
        slopeOverrides = new ArrayList<SlopeOverridePicker>();
        notes = new ArrayList<NotePicker>();
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
}
