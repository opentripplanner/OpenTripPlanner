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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.util.i18n.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is used to localize strings for which localization are known beforehand.
 * Those are local names for:
 * unanamedStreet, corner of x and y, path, bike_path etc.
 *
 * Translations are in src/main/resources/po/*.po
 *
 * locale is set in request.
 *
 * @author mabu
 */
public class LocalizedString implements I18NString, Serializable {

    protected static final Pattern patternMatcher = Pattern.compile("\\{(.*?)\\}");

    private static final Logger LOG = LoggerFactory.getLogger(LocalizedString.class);

    /**
     * This specifies translations it has english text and context if needed
     */
    T localizableKey;

    /**
     * Map which key has which tagNames. Used only when building graph. Cache for {@link #getTagNames()}
     */
    protected transient static ListMultimap<T, String> key_tag_names;

    static {
        key_tag_names = ArrayListMultimap.create();
    }

    //Values with which tagNames are replaced in translations.
    protected String[] params;

    //Used if translation has parameters. Map which parameter is replaced with which value
    private ImmutableMap<String, String> map_params;

    /**
     * Creates String which can be localized
     *  @param translatableObject  object with english text and translation context set in {@link DefaultWayPropertySetSource} and translated with help of gettext.
     * @param params Map with key->values keys are names of parameters in string values are what they are replaced with
     */
    public LocalizedString(T translatableObject, ImmutableMap<String, String> params) {
        this.map_params = params;
        localizableKey = translatableObject;
    }

    /**
     * Creates String which can be localized
     * <p>
     * Uses {@link #getTagNames() } to get which tag values are needed for this key.
     * For each of this tag names tag value is read from OSM way.
     * If tag value is missing it is added as empty string.
     * <p>
     * For example. If key platform has key {ref} current value of tag ref in way is saved to be used in localizations.
     * It currently assumes that tag exists in way. (otherwise this namer wouldn't be used)
     * </p>
     *
     * @param creativeNameLocalPattern Object with english string and translation context for translations set in {@link DefaultWayPropertySetSource}. Translations are with gettext.
     * @param way OSM way from which tag values are read
     */
    public LocalizedString(T creativeNameLocalPattern, OSMWithTags way) {
        this.localizableKey = creativeNameLocalPattern;
        List<String> lparams = new ArrayList<String>(4);
        //Which tags do we want from way
        List<String> tag_names = getTagNames();
        if (tag_names != null && !tag_names.isEmpty()) {
            for(String tag_name: tag_names) {
                String param = way.getTag(tag_name);
                if (param != null) {
                    lparams.add(param);
                } else {
                    lparams.add("");
                }
            }
            this.params = lparams.toArray(new String[lparams.size()]);
        }

    }

    /**
     * Creates LocalizedString without parameters
     *
     * @param translatableObject object with english text and translation context and translated with help of gettext
     */
    public LocalizedString(T translatableObject) {
        this.localizableKey = translatableObject;
    }

    /**
     * Finds wanted tag names in name
     * <p>
     * It uses English localization for key to get tag names.
     * Tag names have to be enclosed in brackets.
     *
     * For example "Platform {ref}" ref is way tagname.
     *
     * </p>
     * @return tagName
     */
    private List<String> getTagNames() {
        //TODO: after finding all keys for replacements replace strings to normal java strings
        //with https://stackoverflow.com/questions/2286648/named-placeholders-in-string-formatting if it is faster
        //otherwise it's converted only when toString is called
        if( key_tag_names.containsKey(localizableKey)) {
            return key_tag_names.get(localizableKey);
        }
        List<String> tag_names = new ArrayList<String>(4);
        String english_trans = ResourceBundleSingleton.INSTANCE.localizeGettext(localizableKey, Locale.ENGLISH);

        Matcher matcher = patternMatcher.matcher(english_trans);
        while (matcher.find()) {
            String tag_name = matcher.group(1);
            key_tag_names.put(localizableKey, tag_name);
            tag_names.add(tag_name);
        }
        return tag_names;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LocalizedString that = (LocalizedString) o;
        return Objects.equals(localizableKey, that.localizableKey) &&
            Objects.equals(params, that.params) &&
            Objects.equals(map_params, that.map_params);
    }

    @Override public int hashCode() {
        return Objects.hash(localizableKey, params, map_params);
    }

    /**
     * Returns translated string in default locale
     * with tag_names replaced with values
     *
     * Default locale is defaultLocale from {@link ResourceBundleSingleton}
     * @return
     */
    @Override
    public String toString() {
        return this.toString(ResourceBundleSingleton.INSTANCE.getDefaultLocale());
    }

    /**
     * Returns translated string in wanted locale
     * with tag_names replaced with values
     *
     * @param locale
     * @return
     */
    @Override public String toString(Locale locale) {
        if (this.localizableKey == null) {
            return null;
        }
        if (map_params != null) {
            return ResourceBundleSingleton.INSTANCE.localizeGettextSprintfFormat(localizableKey, locale, map_params);
        }
        String translation = ResourceBundleSingleton.INSTANCE.localizeGettext(localizableKey, locale);

        if (this.params != null && this.params.length > 0) {
            translation = patternMatcher.matcher(translation).replaceAll("%s");
            return String.format(translation, (Object[]) params);
        } else {
            return translation;
        }
    }
}
