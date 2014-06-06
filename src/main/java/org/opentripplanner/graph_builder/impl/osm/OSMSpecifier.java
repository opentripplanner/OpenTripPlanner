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
import java.util.List;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * Specifies a class of OSM tagged entities (e.g. ways) by a list of tags and their values (which may be wildcards).
 * The OSMSpecifier which matches the most tags on an OSM entity will win. In the event that several OSMSpecifiers
 * match the same number of tags, the one that does so using less wildcards will win. For example, if one OSMSpecifier
 * has the tags (highway=residential, cycleway=*) and another has (highway=residential, surface=paved) and a way has the
 * tags (highway=residential, cycleway=lane, surface=paved) the second OSMSpecifier will be applied to that way
 * (2 exact matches beats 1 exact match and a wildcard match).
 */
public class OSMSpecifier {

    public List<P2<String>> kvpairs;

    public OSMSpecifier() {
        kvpairs = new ArrayList<P2<String>>(); // TODO string-pairs with a proper OSM tag class
    }

    public OSMSpecifier(String spec) {
        this();
        setKvpairs(spec);
    }

    public void setKvpairs(String spec) {
        String[] pairs = spec.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            kvpairs.add(new P2<String>(kv[0], kv[1]));
        }
    }

    /**
     * Calculates a pair of scores expressing how well an OSM entity's tags match this specifier.
     *
     * Tags in this specifier are matched against those for the left and right side of the OSM way separately. See:
     * http://wiki.openstreetmap.org/wiki/Forward_%26_backward,_left_%26_right
     * TODO: we should probably support forward/backward as well.
     * TODO: simply count the number of full, partial, and wildcard matches instead of using a scoring system.
     *
     * @param match an OSM tagged object to compare to this specifier
     */
    public P2<Integer> matchScores(OSMWithTags match) {
        int leftScore = 0, rightScore = 0;
        int leftMatches = 0, rightMatches = 0;
        for (P2<String> pair : kvpairs) {
            // TODO why are we repeatedly converting these to lower case every time they are used?
            // Probably because it used to be possible to set them from Spring XML.
            String tag = pair.getFirst().toLowerCase();
            String value = pair.getSecond().toLowerCase();
            String leftMatchValue = match.getTag(tag + ":left");
            String rightMatchValue = match.getTag(tag + ":right");
            String matchValue = match.getTag(tag);
            if (leftMatchValue == null) {
                leftMatchValue = matchValue;
            }
            if (rightMatchValue == null) {
                rightMatchValue = matchValue;
            }
            int leftTagScore = getTagScore(value, leftMatchValue);
            leftScore += leftTagScore;
            if (leftTagScore > 0) {
                leftMatches ++;
            }
            int rightTagScore = getTagScore(value, rightMatchValue);
            rightScore += rightTagScore;
            if (rightTagScore > 0) {
                rightMatches ++;
            }
        }
        int allMatchLeftBonus = (leftMatches == kvpairs.size()) ? 10 : 0;
        leftScore += allMatchLeftBonus;
        int allMatchRightBonus = (rightMatches == kvpairs.size()) ? 10 : 0;
        rightScore += allMatchRightBonus;
        P2<Integer> score = new P2<Integer>(leftScore, rightScore);
        return score;
    }

    /**
     * Calculates a score expressing how well an OSM entity's tags match this specifier.
     * This does exactly the same thing as matchScores but without regard for :left and :right.
     */
    public int matchScore(OSMWithTags match) {
        int score = 0;
        int matches = 0;
        for (P2<String> pair : kvpairs) {
            String tag = pair.getFirst().toLowerCase();
            String value = pair.getSecond().toLowerCase();
            String matchValue = match.getTag(tag);
            int tagScore = getTagScore(value, matchValue);
            score += tagScore;
            if (tagScore > 0) {
                matches += 1;
            }
        }
        score += matches == kvpairs.size() ? 10 : 0;
        return score;
    }

    /**
     * Calculates a score indicating how well an OSM tag value matches the given matchValue.
     * An exact match is worth 100 points, a partial match on the part of the value before a colon is worth 75 points,
     * and a wildcard match is worth only one point, to serve as a tiebreaker. A score of 0 means they do not match.
     */
    private int getTagScore(String value, String matchValue) {
        // either this matches on a wildcard, or it matches exactly
        if (value.equals("*") && matchValue != null) {
            return 1; // wildcard matches are basically tiebreakers
        } else if (value.equals(matchValue)) {
            return 100;
        } else {
            if (value.contains(":")) {
                // treat cases like cobblestone:flattened as cobblestone if a more-specific match
                // does not apply
                value = value.split(":", 2)[0];
                if (value.equals(matchValue)) {
                    return 75;
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        }
    }

    public void addTag(String key, String value) {
        kvpairs.add(new P2<String>(key, value));
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (P2<String> pair : kvpairs) {
            builder.append(pair.getFirst());
            builder.append("=");
            builder.append(pair.getSecond());
            builder.append(";");
        }
        builder.deleteCharAt(builder.length() - 1); // remove trailing semicolon
        return builder.toString();
    }
}
