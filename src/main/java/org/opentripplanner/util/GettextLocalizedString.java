package org.opentripplanner.util;

import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.util.i18n.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Created by mabu on 4.8.2015.
 */
public class GettextLocalizedString extends LocalizedString {

    private static final Logger LOG = LoggerFactory.getLogger(GettextLocalizedString.class);

    T localizedKey;

    /**
     * Creates String which can be localized
     *
     * @param translatableObject  object with english text and translation context set in {@link DefaultWayPropertySetSource} and translated with help of gettext.
     * @param params Values with which tagNames are replaced in translations.
     */
    public GettextLocalizedString(T translatableObject, String[] params) {
        super(translatableObject.msgid, params);
        //FIXME: this doesn't support context yet
        localizedKey = translatableObject;
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
    public GettextLocalizedString(T creativeNameLocalPattern, OSMWithTags way) {
        this.localizedKey = creativeNameLocalPattern;
        this.key = creativeNameLocalPattern.msgid;
        List<String> lparams = new ArrayList<String>(4);
        //Which tags do we want from way
        List<String> tag_names = getTagNames();
        if (tag_names != null) {
            for(String tag_name: tag_names) {
                String param = way.getTag(tag_name);
                if (param != null) {
                    lparams.add(param);
                } else {
                    lparams.add("");
                }
            }
        }
        this.params = lparams.toArray(new String[lparams.size()]);
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
    @Override protected List<String> getTagNames() {
        //TODO: after finding all keys for replacements replace strings to normal java strings
        //with https://stackoverflow.com/questions/2286648/named-placeholders-in-string-formatting if it is faster
        //otherwise it's converted only when toString is called
        if( key_tag_names.containsKey(key)) {
            return key_tag_names.get(key);
        }
        List<String> tag_names = new ArrayList<String>(4);
        String english_trans = this.key;

        Matcher matcher = patternMatcher.matcher(english_trans);
        while (matcher.find()) {
            String tag_name = matcher.group(1);
            key_tag_names.put(key, tag_name);
            tag_names.add(tag_name);
        }
        return tag_names;
    }
    /**
     * Returns translated string in wanted locale
     * with tag_names replaced with values
     *
     * @param locale
     * @return
     */
    @Override public String toString(Locale locale) {
        if (this.key == null || this.localizedKey == null) {
            return null;
        }
        String translation = ResourceBundleSingleton.INSTANCE.localizeGettext(localizedKey, locale);

        if (this.params != null) {
            translation = patternMatcher.matcher(translation).replaceAll("%s");
            return String.format(translation, (Object[]) params);
        } else {
            return translation;
        }
    }
}
