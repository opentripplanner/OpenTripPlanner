package org.opentripplanner.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * This is used to localize strings for which localization are known beforehand. Those are local
 * names for: unanamedStreet, corner of x and y, path, bike_path etc.
 * <p>
 * Translations are in src/main/resources/WayProperties_lang.properties and
 * internals_lang.properties
 * <p>
 * locale is set in request.
 *
 * @author mabu
 */
public class LocalizedString implements I18NString, Serializable {

  private static final Pattern patternMatcher = Pattern.compile("\\{(.*?)}");

  /**
   * Map which key has which tagNames. Used only when building graph.
   */
  private static final transient ListMultimap<String, String> key_tag_names;
  //Key which specifies translation
  private final String key;
  //Values with which tagNames are replaced in translations.
  private final I18NString[] params;

  static {
    key_tag_names = ArrayListMultimap.create();
  }

  /**
   * Creates String which can be localized
   *
   * @param key key of translation for this way set in {@link org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource}
   *            and translations read from properties Files
   */
  public LocalizedString(String key) {
    this.key = key;
    this.params = null;
  }

  /**
   * Creates String which can be localized
   *
   * @param key    key of translation for this way set in {@link org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource}
   *               and translations read from properties Files
   * @param params Values with which tagNames are replaced in translations.
   */
  public LocalizedString(String key, I18NString... params) {
    this.key = key;
    this.params = params;
  }

  /**
   * Creates String which can be localized
   * <p>
   * Uses {@link #getTagNames() } to get which tag values are needed for this key. For each of this
   * tag names tag value is read from OSM way. If tag value is missing it is added as empty string.
   * <p>
   * For example. If key platform has key {ref} current value of tag ref in way is saved to be used
   * in localizations. It currently assumes that tag exists in way. (otherwise this namer wouldn't
   * be used)
   * </p>
   *
   * @param key key of translation for this way set in {@link org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource}
   *            and translations read from from properties Files
   * @param way OSM way from which tag values are read
   */
  public LocalizedString(String key, OSMWithTags way) {
    this.key = key;
    List<I18NString> lparams = new ArrayList<>(4);
    //Which tags do we want from way
    List<String> tag_names = getTagNames();
    for (String tag_name : tag_names) {
      String param = way.getTag(tag_name);
      lparams.add(new NonLocalizedString(Objects.requireNonNullElse(param, "")));
    }

    this.params = lparams.toArray(new I18NString[0]);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(key);
    result = 31 * result + Arrays.hashCode(params);
    return result;
  }

  @Override
  public boolean equals(Object other) {
    return (
      other instanceof LocalizedString &&
      key.equals(((LocalizedString) other).key) &&
      Arrays.equals(params, ((LocalizedString) other).params)
    );
  }

  /**
   * Returns translated string in default locale with tag_names replaced with values
   * <p>
   * Default locale is defaultLocale from {@link ResourceBundleSingleton}
   */
  @Override
  public String toString() {
    return this.toString(null);
  }

  /**
   * Returns translated string in wanted locale with tag_names replaced with values
   */
  @Override
  public String toString(Locale locale) {
    if (this.key == null) {
      return null;
    }
    //replaces {name}, {ref} etc with %s to be used as parameters
    //in string formatting with values from way tags values
    String translation = ResourceBundleSingleton.INSTANCE.localize(this.key, locale);
    if (this.params != null) {
      translation = patternMatcher.matcher(translation).replaceAll("%s");
      return String.format(
        translation,
        Arrays.stream(params).map(i -> i.toString(locale)).toArray(Object[]::new)
      );
    } else {
      return translation;
    }
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
   *
   * @return tagName
   */
  private List<String> getTagNames() {
    //TODO: after finding all keys for replacements replace strings to normal java strings
    //with https://stackoverflow.com/questions/2286648/named-placeholders-in-string-formatting if it is faster
    //otherwise it's converted only when toString is called
    if (key_tag_names.containsKey(key)) {
      return key_tag_names.get(key);
    }
    List<String> tag_names = new ArrayList<>(4);
    String english_trans = ResourceBundleSingleton.INSTANCE.localize(this.key, Locale.ENGLISH);

    Matcher matcher = patternMatcher.matcher(english_trans);
    while (matcher.find()) {
      String tag_name = matcher.group(1);
      key_tag_names.put(key, tag_name);
      tag_names.add(tag_name);
    }
    return tag_names;
  }
}
