package org.opentripplanner.graph_builder.module.osm;

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

    /**
     * Replace various pattern by the OSM tag values, with I18n support.
     * 
     * @param pattern Pattern containing options tags to replace, such as "text" or "note: {note}".
     *        Tag names between {} are replaced by the OSM tag value, if it is present (or the empty
     *        string if not).
     * @param way The way containing the tag values
     * @return A map language code -> text, with at least one entry for the default language, and
     *         any other language found in OSM tag.
     */
    public static Map<String, String> generateI18N(String pattern, OSMWithTags way) {

        if (pattern == null)
            return null;

        Map<String, StringBuffer> i18n = new HashMap<String, StringBuffer>();
        i18n.put(null, new StringBuffer());
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
                            i18n.put(lang, new StringBuffer(i18n.get(null)));
                    }
                }
            }
            // get the simple value (eg: description=...)
            String defTag = way.getTag(defKey);
            if (defTag == null && i18nTags != null && i18nTags.size() != 0) {
                defTag = i18nTags.values().iterator().next();
            }
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
