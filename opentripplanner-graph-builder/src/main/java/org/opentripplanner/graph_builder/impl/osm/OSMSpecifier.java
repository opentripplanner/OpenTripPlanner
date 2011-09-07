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
import org.opentripplanner.graph_builder.model.osm.OSMWithTags;

/** Specifies a class of OSM tagged objects (e.g. ways) by a list tags and their values */
public class OSMSpecifier {
	public List<P2<String>> kvpairs;
	
	public OSMSpecifier() {
		kvpairs = new ArrayList<P2<String>>();
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
	 * Returns a score comparing how well the parameter matches
	 * this specifier 
	 * @param match an OSM tagged object to compare to this specifier 
	 * @return
	 */
	public int matchScore(OSMWithTags match) {
		int score = 0;
		for (P2<String> pair : kvpairs) {
      String tag = pair.getFirst().toLowerCase();
			String value = pair.getSecond().toLowerCase();

			String matchValue = match.getTag(tag);
			//either this matches on a wildcard, or it matches exactly
			if (value.equals("*") && matchValue != null) {
				score += 1; //wildcard matches are basically tiebreakers
			} else if (value.equals(matchValue)) {
				score += 100;
			} else {
				if (value.contains(":")) {
					//treat cases like cobblestone:flattened as cobblestone if a more-specific match
					//does not apply
					value = value.split(":", 2)[0];
				} else if (value.equals(matchValue)) {
					score += 75;
				} else {
					return 0;
				}
			}
			
		}
		return score;
	}

	public void addTag(String key, String value) {
		kvpairs.add(new P2<String>(key, value));
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (P2<String> pair: kvpairs) {
			builder.append(pair.getFirst());
			builder.append("=");
			builder.append(pair.getSecond());
			builder.append(";");
		}
		builder.deleteCharAt(builder.length() - 1); //remove trailing semicolon
		return builder.toString();
	}
}
