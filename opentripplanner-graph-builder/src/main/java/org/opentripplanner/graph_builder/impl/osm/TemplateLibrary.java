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

package org.opentripplanner.graph_builder.impl.osm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class TemplateLibrary {
    private static final Matcher matcher = Pattern.compile("\\{(.*?)\\}").matcher("");

    public static String generate(String pattern, OSMWithTags way) {

        if (pattern == null) {
            return null;
        }
        StringBuffer gen_name = new StringBuffer();

        matcher.reset(pattern);
        int lastEnd = 0;
        while (matcher.find()) {
            // add the stuff before the match
            gen_name.append(pattern, lastEnd, matcher.start());
            lastEnd = matcher.end();
            // and then the value for the match
            String key = matcher.group(1);
            String tag = way.getTag(key);
            if (tag != null) {
                gen_name.append(tag);
            }
        }
        gen_name.append(pattern, lastEnd, pattern.length());

        return gen_name.toString();
    }
}
