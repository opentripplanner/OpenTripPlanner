package org.opentripplanner.osm.wayproperty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.resources.ResourceBundleSingleton;
import org.opentripplanner.osm.model.OsmEntity;

class LocalizedStringMapper {

  private static final LocalizedStringMapper INSTANCE = new LocalizedStringMapper();

  private static final Pattern PATTERN_MATCHER = Pattern.compile("\\{(.*?)}");

  /**
   * Map which key has which tagNames. Used only when building graph.
   */
  private final ListMultimap<String, String> keyTagNames = ArrayListMultimap.create();

  static LocalizedStringMapper getInstance() {
    return INSTANCE;
  }

  /**
   * Creates String which can be localized.
   * <p>
   * Uses {@link #getTagNames(String) } to get which tag values are needed for this key. For each of
   * this tag names tag value is read from OSM way. If tag value is missing it is added as empty
   * string.
   * <p>
   * For example. If key platform has key {ref} current value of tag ref in way is saved to be used
   * in localizations. It currently assumes that tag exists in way. (otherwise this namer wouldn't
   * be used)
   * </p>
   *
   * @param key key of translation for this way set in the WayPropertyMapper and translations read
   *            from properties Files
   * @param way OSM way from which tag values are read
   */
  LocalizedString map(String key, OsmEntity way) {
    List<I18NString> lparams = new ArrayList<>(4);
    //Which tags do we want from way
    List<String> tagNames = getTagNames(key);
    for (String it : tagNames) {
      String param = way.getTag(it);
      lparams.add(new NonLocalizedString(Objects.requireNonNullElse(param, "")));
    }

    return new LocalizedString(key, lparams.toArray(new I18NString[0]));
  }

  /**
   * Finds wanted tag names in name
   * <p>
   * It uses English localization for key to get tag names. Tag names have to be enclosed in
   * brackets.
   * <p>
   * For example "Platform {ref}" ref is way tagname.
   *
   * </p>
   */
  private List<String> getTagNames(String key) {
    //TODO: after finding all keys for replacements replace strings to normal java strings
    //with https://stackoverflow.com/questions/2286648/named-placeholders-in-string-formatting if
    // it is faster otherwise it's converted only when toString is called
    if (keyTagNames.containsKey(key)) {
      return keyTagNames.get(key);
    }
    List<String> tagNames = new ArrayList<>(4);
    String englishTrans = ResourceBundleSingleton.INSTANCE.localize(key, Locale.ENGLISH);

    Matcher matcher = PATTERN_MATCHER.matcher(englishTrans);
    while (matcher.find()) {
      String tagName = matcher.group(1);
      keyTagNames.put(key, tagName);
      tagNames.add(tagName);
    }
    return tagNames;
  }
}
