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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class TemplateLibrary {
    private static final Pattern patternMatcher = Pattern.compile("\\{(.*?)\\}");

    public static String generate(String pattern, OSMWithTags way) {

        if (pattern == null) {
            return null;
        }
        StringBuffer gen_name = new StringBuffer();

        Matcher matcher = patternMatcher.matcher(pattern);

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

    public static Map<String, String> generateI18N(String pattern, OSMWithTags way,
            String defaultLang) {

        if (pattern == null)
            return null;

        Map<String, StringBuffer> i18n = new HashMap<String, StringBuffer>();
        i18n.put(defaultLang, new StringBuffer());
        Matcher matcher = patternMatcher.matcher(pattern);

        int lastEnd = 0;
        while (matcher.find()) {
            // add the stuff before the match
            for (StringBuffer sb : i18n.values())
                sb.append(pattern, lastEnd, matcher.start());
            lastEnd = matcher.end();
            // and then the value for the match
            String defKey = matcher.group(1);
            // scan all translated tags
            Map<String, String> i18nTags = way.getTagsByPrefix(defKey);
            if (i18nTags != null) {
                for (Map.Entry<String, String> kv : i18nTags.entrySet()) {
                    if (!kv.getKey().equals(defKey)) {
                        String lang = kv.getKey().substring(defKey.length() + 1);
                        if (!i18n.containsKey(lang))
                            i18n.put(lang, new StringBuffer(i18n.get(defaultLang)));
                    }
                }
            }
            // get the simple value (eg: description=...)
            String defTag = way.getTag(defKey);
            // get the translated value, if exists
            for (String lang : i18n.keySet()) {
                String i18nTag = way.getTag(defKey + ":" + lang);
                i18n.get(lang).append(i18nTag != null ? i18nTag : (defTag != null ? defTag : ""));
            }
        }
        for (StringBuffer sb : i18n.values())
            sb.append(pattern, lastEnd, pattern.length());
        Map<String, String> out = new HashMap<String, String>(i18n.size());
        for (Map.Entry<String, StringBuffer> kv : i18n.entrySet())
            out.put(kv.getKey(), kv.getValue().toString());
        return out;
    }
}
