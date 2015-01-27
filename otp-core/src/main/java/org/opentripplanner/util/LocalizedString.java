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

package org.opentripplanner.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 *
 * @author mabu
 */
public class LocalizedString implements I18NString, Serializable {
    private static final Pattern pattern = Pattern.compile("\\{(.*?)\\}");
    private static final Matcher matcher = pattern.matcher("");
    
    /**
     * Map which key has which tagName. Used only when building graph.
     */
    private transient static Map<String, String> key_params;
    
    static {
        key_params = new HashMap<String, String>();
    }
    
    private String key;
    private String[] params;

    /**
     * Creates String which can be localized
     * @param key key of translation for this way set in {@link DefaultWayPropertySetSource} and translations read from from properties Files
     * @param params Values with which tagNames are replaced in translations.
     */
    public LocalizedString(String key, String[] params) {
        this.key = key;
        this.params = params;
    }

    /**
     * Creates String which can be localized
     * <p>
     * Uses {@link #getTagNames() } to get which tag values are needed for this key.
     * For each of this tag names tag value is get from OSM way.
     * 
     * For example. If key platform has key ref current value of tag ref in way is saved to be used in localizations.
     * It currently assumes that tag exists in way. (otherwise this namer wouldn't be used)
     * </p>
     * @param key key of translation for this way set in {@link DefaultWayPropertySetSource} and translations read from from properties Files
     * @param way OSM way from which tag values are read
     */
    public LocalizedString(String key, OSMWithTags way) {
        this.key = key;
        List<String> lparams = new ArrayList<String>(1);
        //Which tags do we want from way
        String tag_name = getTagNames();
        if (tag_name != null) {
            //Tag value
            String param = way.getTag(tag_name);
            if (param != null) {
                lparams.add(param);
                this.params = lparams.toArray(new String[lparams.size()]);
            }
        }
    }
    
    /**
     * Finds wanted tag names in name
     * <p>
     * It uses English localization for key to get tag names.
     * Tag names have to be enclosed in brackets.
     * 
     * For example "Platform {ref}" ref is way tagname.
     * 
     * NOTE: Only one tag name is currently supported.
     * </p>
     * @return tagName
     */
    private String getTagNames() {
        //TODO: after finding all keys for replacements replace strings to normal java strings
        //with https://stackoverflow.com/questions/2286648/named-placeholders-in-string-formatting if it is faster
        //otherwise it's converted only when toString is called
        if( key_params.containsKey(key)) {
            return key_params.get(key);
        }
        String english_trans = ResourceBundleSingleton.INSTANCE.localize(this.key, Locale.ENGLISH);
        matcher.reset(english_trans);
        int lastEnd = 0;
        while (matcher.find()) {
            
            lastEnd = matcher.end();
            // and then the value for the match
            String m_key = matcher.group(1);
            key_params.put(key, m_key);
            return m_key;
        }
        return null;
    }

    @Override
    public String toString() {
        return this.toString(ResourceBundleSingleton.INSTANCE.getDefaultLocale());
    }    

    @Override
    public String toString(Locale locale) {
        if (this.key == null) {
            return null;
        }
        //replaces {name}, {ref} etc with %s to be used as parameters
        //in string formatting with values from way tags values
        String translation = ResourceBundleSingleton.INSTANCE.localize(this.key, locale);
        if (this.params != null) {
            translation = pattern.matcher(translation).replaceFirst("%s");
            return String.format(translation, (Object[]) params);
        } else {
            return translation;
        }
    }

}
