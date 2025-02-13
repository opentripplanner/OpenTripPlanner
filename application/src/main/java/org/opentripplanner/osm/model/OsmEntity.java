package org.opentripplanner.osm.model;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A base class for OSM entities containing common methods.
 */
public class OsmEntity {

  /**
   * highway=* values that we don't want to even consider when building the graph.
   */
  private static final Set<String> NON_ROUTABLE_HIGHWAYS = Set.of(
    "proposed",
    "planned",
    "construction",
    "razed",
    "raceway",
    "abandoned",
    "historic",
    "no",
    "emergency_bay",
    "rest_area",
    "services",
    "bus_guideway",
    "escape"
  );

  private static final Set<String> INDOOR_ROUTABLE_VALUES = Set.of("corridor", "area");

  private static final Set<String> LEVEL_TAGS = Set.of("level", "layer");
  private static final Set<String> DEFAULT_LEVEL = Set.of("0");
  private static final Consumer<String> NO_OP = i -> {};

  /* To save memory this is only created when an entity actually has tags. */
  private Map<String, String> tags;

  protected long id;

  protected I18NString creativeName;

  private OsmProvider osmProvider;

  public static boolean isFalse(String tagValue) {
    return ("no".equals(tagValue) || "0".equals(tagValue) || "false".equals(tagValue));
  }

  public static boolean isTrue(String tagValue) {
    return ("yes".equals(tagValue) || "1".equals(tagValue) || "true".equals(tagValue));
  }

  /**
   * Gets the id.
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the id.
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Adds a tag.
   */
  public void addTag(OsmTag tag) {
    if (tags == null) tags = new HashMap<>();

    tags.put(tag.getK().toLowerCase(), tag.getV());
  }

  /**
   * Adds a tag.
   */
  public OsmEntity addTag(String key, String value) {
    if (key == null || value == null) {
      return this;
    }

    if (tags == null) {
      tags = new HashMap<>();
    }

    tags.put(key.toLowerCase(), value);
    return this;
  }

  /**
   * The tags of an entity.
   */
  public Map<String, String> getTags() {
    return Objects.requireNonNullElse(tags, Map.of());
  }

  /**
   * Is the tag defined?
   */
  public boolean hasTag(String tag) {
    tag = tag.toLowerCase();
    return tags != null && tags.containsKey(tag);
  }

  /**
   * Determines if a tag contains a false value. 'no', 'false', and '0' are considered false.
   */
  public boolean isTagFalse(String tag) {
    tag = tag.toLowerCase();
    if (tags == null) {
      return false;
    }

    return isFalse(getTag(tag));
  }

  /**
   * Returns the level of wheelchair access of the element.
   */
  public Accessibility wheelchairAccessibility() {
    if (isTagTrue("wheelchair")) {
      return Accessibility.POSSIBLE;
    } else if (isTagFalse("wheelchair")) {
      return Accessibility.NOT_POSSIBLE;
    } else {
      return Accessibility.NO_INFORMATION;
    }
  }

  /**
   * Determines if a tag contains a true value. 'yes', 'true', and '1' are considered true.
   */
  public boolean isTagTrue(String tag) {
    tag = tag.toLowerCase();
    if (tags == null) {
      return false;
    }

    return isTrue(getTag(tag));
  }

  /**
   * Returns true if bicycle dismounts are forced.
   */
  public boolean isBicycleDismountForced() {
    return isTag("bicycle", "dismount");
  }

  public boolean isSidewalk() {
    return isTag("footway", "sidewalk") && isTag("highway", "footway");
  }

  protected boolean doesTagAllowAccess(String tag) {
    if (tags == null) {
      return false;
    }
    if (isTagTrue(tag)) {
      return true;
    }
    tag = tag.toLowerCase();
    String value = getTag(tag);
    return (
      "designated".equals(value) ||
      "official".equals(value) ||
      "permissive".equals(value) ||
      "unknown".equals(value)
    );
  }

  /** @return a tag's value, converted to lower case. */
  @Nullable
  public String getTag(String tag) {
    tag = tag.toLowerCase();
    if (tags != null && tags.containsKey(tag)) {
      return tags.get(tag);
    }
    return null;
  }

  /**
   *
   * @return A tags value converted to lower case. An empty Optional if tags is not present.
   */
  public Optional<String> getTagOpt(String network) {
    return Optional.ofNullable(getTag(network));
  }

  /**
   * Get tag and convert it to an integer. If the tag exist, but can not be parsed into a number,
   * then the error handler is called with the value which failed to parse.
   */
  public OptionalInt getTagAsInt(String tag, Consumer<String> errorHandler) {
    String value = getTag(tag);
    if (value != null) {
      try {
        return OptionalInt.of(Integer.parseInt(value));
      } catch (NumberFormatException e) {
        errorHandler.accept(value);
      }
    }
    return OptionalInt.empty();
  }

  /**
   * Parse an OSM duration tag, which is one of:
   *   mm
   *   hh:mm
   *   hh:mm:ss
   * and where the leading value is not limited to any maximum.
   * See <a href="https://wiki.openstreetmap.org/wiki/Key:duration">OSM wiki definition
   * of duration</a>.
   *
   * @param duration string in format mm, hh:mm, or hh:mm:ss
   * @return Duration
   * @throws DateTimeParseException on bad input
   */
  public static Duration parseOsmDuration(String duration) {
    // Unfortunately DateFormatParserBuilder doesn't quite do enough for this case.
    // It has the capability for expressing optional parts, so it could express hh(:mm(:ss)?)?
    // but it cannot express (hh:)?mm(:ss)? where the existence of (:ss) implies the existence
    // of (hh:). Even if it did, it would not be able to handle the cases where hours are
    // greater than 23 or (if there is no hours part at all) minutes are greater than 59, which
    // are both allowed by the spec and exist in OSM data. Durations are not LocalTimes after
    // all, in parsing a LocalTime it makes sense and is correct that hours cannot be more than
    // 23 or minutes more than 59, but in durations if you have capped the largest unit, it is
    // reasonable for the amount of the largest unit to be as large as it needs to be.
    int colonCount = (int) duration.chars().filter(ch -> ch == ':').count();
    if (colonCount <= 2) {
      try {
        int i, j;
        long hours, minutes, seconds;
        // The first :-separated element can be any width, and has no maximum. It still has
        // to be non-negative. The following elements must be 2 characters wide, non-negative,
        // and less than 60.
        switch (colonCount) {
          // case "m"
          case 0:
            minutes = Long.parseLong(duration);
            if (minutes >= 0) {
              return Duration.ofMinutes(minutes);
            }
            break;
          // case "h:mm"
          case 1:
            i = duration.indexOf(':');
            hours = Long.parseLong(duration.substring(0, i));
            minutes = Long.parseLong(duration.substring(i + 1));
            if (duration.length() - i == 3 && hours >= 0 && minutes >= 0 && minutes < 60) {
              return Duration.ofHours(hours).plusMinutes(minutes);
            }
            break;
          // case "h:mm:ss"
          default:
            i = duration.indexOf(':');
            j = duration.indexOf(':', i + 1);
            hours = Long.parseLong(duration.substring(0, i));
            minutes = Long.parseLong(duration.substring(i + 1, j));
            seconds = Long.parseLong(duration.substring(j + 1));
            if (
              j - i == 3 &&
              duration.length() - j == 3 &&
              hours >= 0 &&
              minutes >= 0 &&
              minutes < 60 &&
              seconds >= 0 &&
              seconds < 60
            ) {
              return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
            }
            break;
        }
      } catch (NumberFormatException e) {
        // fallthrough
      }
    }
    throw new DateTimeParseException("Bad OSM duration", duration, 0);
  }

  /**
   * Gets a tag's value, assumes it is an OSM wiki specified duration, parses and returns it.
   * If parsing fails, calls the error handler.
   *
   * @param key
   * @param errorHandler
   * @return parsed Duration, or empty
   */
  public Optional<Duration> getTagValueAsDuration(String key, Consumer<String> errorHandler) {
    String value = getTag(key);
    if (value != null) {
      try {
        return Optional.of(parseOsmDuration(value));
      } catch (DateTimeParseException e) {
        errorHandler.accept(value);
      }
    }
    return Optional.empty();
  }

  /**
   * Some tags are allowed to have values like 55, "true" or "false".
   * <p>
   * "true", "yes" is returned as 1.
   * <p>
   * "false", "no" is returned as 0
   * <p>
   * Everything else is returned as an emtpy optional.
   */
  public OptionalInt parseIntOrBoolean(String tag, Consumer<String> errorHandler) {
    var maybeInt = getTagAsInt(tag, NO_OP);
    if (maybeInt.isPresent()) {
      return maybeInt;
    } else {
      if (isTagTrue(tag)) {
        return OptionalInt.of(1);
      } else if (isTagFalse(tag)) {
        return OptionalInt.of(0);
      } else if (hasTag(tag)) {
        errorHandler.accept(getTag(tag));
        return OptionalInt.empty();
      } else {
        return OptionalInt.empty();
      }
    }
  }

  /**
   * Checks is a tag contains the specified value.
   */
  public boolean isTag(String tag, String value) {
    tag = tag.toLowerCase();
    if (tags != null && tags.containsKey(tag) && value != null) {
      return value.equals(tags.get(tag));
    }

    return false;
  }

  /**
   * Takes a tag key and checks if the value is any of those in {@code oneOfTags}.
   */
  public boolean isOneOfTags(String key, Set<String> oneOfTags) {
    return oneOfTags.stream().anyMatch(value -> isTag(key, value));
  }

  /**
   * Returns a name-like value for an entity (if one exists). The otp: namespaced tags are created
   * by {@link OsmModule}
   */
  @Nullable
  public I18NString getAssumedName() {
    if (tags == null) {
      return null;
    }
    if (tags.containsKey("name")) {
      return TranslatedString.getI18NString(this.generateI18NForPattern("{name}"), true, false);
    }
    if (tags.containsKey("otp:route_name")) {
      return new NonLocalizedString(tags.get("otp:route_name"));
    }
    if (this.creativeName != null) {
      return this.creativeName;
    }
    if (tags.containsKey("otp:route_ref")) {
      return new NonLocalizedString(tags.get("otp:route_ref"));
    }
    if (tags.containsKey("ref")) {
      return new NonLocalizedString(tags.get("ref"));
    }
    return null;
  }

  /**
   * Replace various pattern by the OSM tag values, with I18n support.
   *
   * @param pattern Pattern containing options tags to replace, such as "text" or "note: {note}".
   *                Tag names between {} are replaced by the OSM tag value, if it is present (or the
   *                empty string if not).
   * @return A map language code → text, with at least one entry for the default language, and any
   * other language found in OSM tag.
   */
  public Map<String, String> generateI18NForPattern(String pattern) {
    Map<String, StringBuffer> i18n = new HashMap<>();
    i18n.put(null, new StringBuffer());
    Matcher matcher = Pattern.compile("\\{(.*?)}").matcher(pattern);

    int lastEnd = 0;
    while (matcher.find()) {
      // add the stuff before the match
      for (StringBuffer sb : i18n.values()) sb.append(pattern, lastEnd, matcher.start());
      lastEnd = matcher.end();
      // and then the value for the match
      String defKey = matcher.group(1);
      // scan all translated tags
      Map<String, String> i18nTags = getTagsByPrefix(defKey);
      for (Map.Entry<String, String> kv : i18nTags.entrySet()) {
        if (!kv.getKey().equals(defKey)) {
          String lang = kv.getKey().substring(defKey.length() + 1);
          if (!i18n.containsKey(lang)) i18n.put(lang, new StringBuffer(i18n.get(null)));
        }
      }
      // get the simple value (eg: description=...)
      String defTag = getTag(defKey);
      if (defTag == null && !i18nTags.isEmpty()) {
        defTag = i18nTags.values().iterator().next();
      }
      // get the translated value, if exists
      for (String lang : i18n.keySet()) {
        String i18nTag = getTag(defKey + ":" + lang);
        i18n.get(lang).append(i18nTag != null ? i18nTag : (defTag != null ? defTag : ""));
      }
    }
    for (StringBuffer sb : i18n.values()) sb.append(pattern, lastEnd, pattern.length());
    Map<String, String> out = new HashMap<>(i18n.size());
    for (Map.Entry<String, StringBuffer> kv : i18n.entrySet()) out.put(
      kv.getKey(),
      kv.getValue().toString()
    );
    return out;
  }

  private Map<String, String> getTagsByPrefix(String prefix) {
    Map<String, String> out = new HashMap<>();
    for (Map.Entry<String, String> entry : tags.entrySet()) {
      String k = entry.getKey();
      if (k.equals(prefix) || k.startsWith(prefix + ":")) {
        out.put(k, entry.getValue());
      }
    }

    return out;
  }

  /**
   * Returns true if this element is under construction.
   */
  public boolean isUnderConstruction() {
    String highway = getTag("highway");
    String cycleway = getTag("cycleway");
    return "construction".equals(highway) || "construction".equals(cycleway);
  }

  /**
   * Returns true if access is generally denied to this element (potentially with exceptions).
   */
  public boolean isGeneralAccessDenied() {
    return isTagDeniedAccess("access");
  }

  /**
   * Returns true if cars are explicitly denied access.
   */
  public boolean isMotorcarExplicitlyDenied() {
    return isTagDeniedAccess("motorcar");
  }

  /**
   * Returns true if cars are explicitly allowed.
   */
  public boolean isMotorcarExplicitlyAllowed() {
    return doesTagAllowAccess("motorcar");
  }

  /**
   * Returns true if cars/motorcycles/HGV are explicitly denied access.
   */
  public boolean isMotorVehicleExplicitlyDenied() {
    return isTagDeniedAccess("motor_vehicle");
  }

  /**
   * Returns true if cars/motorcycles/HGV are explicitly allowed.
   */
  public boolean isMotorVehicleExplicitlyAllowed() {
    return doesTagAllowAccess("motor_vehicle");
  }

  /**
   * Returns true if all land vehicles (including bicycles) are explicitly denied access.
   */
  public boolean isVehicleExplicitlyDenied() {
    return isTagDeniedAccess("vehicle");
  }

  /**
   * Returns true if all land vehicles (including bicycles) are explicitly allowed.
   */
  public boolean isVehicleExplicitlyAllowed() {
    return doesTagAllowAccess("vehicle");
  }

  /**
   * Returns true if bikes are explicitly denied access.
   * <p>
   * bicycle is denied if bicycle:no, bicycle:dismount or bicycle:license.
   */
  public boolean isBicycleExplicitlyDenied() {
    return (isTagDeniedAccess("bicycle") || "dismount".equals(getTag("bicycle")));
  }

  /**
   * Returns true if bikes are explicitly allowed.
   */
  public boolean isBicycleExplicitlyAllowed() {
    return doesTagAllowAccess("bicycle");
  }

  /**
   * Returns true if pedestrians are explicitly denied access.
   */
  public boolean isPedestrianExplicitlyDenied() {
    return isTagDeniedAccess("foot");
  }

  /**
   * Returns true if pedestrians are explicitly allowed.
   */
  public boolean isPedestrianExplicitlyAllowed() {
    return doesTagAllowAccess("foot");
  }

  /**
   * @return True if this node / area is a parking.
   */
  public boolean isParking() {
    return isTag("amenity", "parking");
  }

  /**
   * @return True if this node / area is a park and ride.
   */
  public boolean isParkAndRide() {
    String parkingType = getTag("parking");
    String parkAndRide = getTag("park_ride");
    return (
      isParking() &&
      (
        (parkingType != null && parkingType.contains("park_and_ride")) ||
        (parkAndRide != null && !parkAndRide.equalsIgnoreCase("no"))
      )
    );
  }

  /**
   * Is this a public transport boarding location where passengers wait for transit and that can be
   * linked to a transit stop vertex later on.
   * <p>
   * This intentionally excludes railway=stop and public_transport=stop because these are supposed
   * to be placed on the tracks not on the platform.
   *
   * @return whether the node is a place used to board a public transport vehicle
   */
  public boolean isBoardingLocation() {
    return (
      isTag("highway", "bus_stop") ||
      isTag("railway", "tram_stop") ||
      isTag("railway", "station") ||
      isTag("railway", "halt") ||
      isTag("amenity", "bus_station") ||
      isTag("amenity", "ferry_terminal") ||
      isPlatform()
    );
  }

  /**
   * Determines if an entity is a platform.
   * <p>
   * However, they are filtered out if they are tagged usage=tourism. This prevents miniature tourist
   * railways like the one in Portland's Zoo (https://www.openstreetmap.org/way/119108622)
   * from being linked to transit stops that are underneath it.
   **/
  public boolean isPlatform() {
    var isPlatform = isTag("public_transport", "platform") || isRailwayPlatform();
    return isPlatform && !isTag("usage", "tourism");
  }

  public boolean isRailwayPlatform() {
    return isTag("railway", "platform");
  }

  /**
   * @return True if this entity provides an entrance to a platform or similar entity
   */
  public boolean isEntrance() {
    return (
      (
        isTag("railway", "subway_entrance") ||
        isTag("highway", "elevator") ||
        isTag("entrance", "yes") ||
        isTag("entrance", "main")
      ) &&
      !isTag("access", "private") &&
      !isTag("access", "no")
    );
  }

  /**
   * @return True if this node / area is a bike parking.
   */
  public boolean isBikeParking() {
    return (
      isTag("amenity", "bicycle_parking") && !isTag("access", "private") && !isTag("access", "no")
    );
  }

  public void setCreativeName(I18NString creativeName) {
    this.creativeName = creativeName;
  }

  @Nullable
  public String url() {
    return null;
  }

  /**
   * Returns all non-empty values of the tags passed in as input values.
   * <p>
   * Values are split by semicolons.
   */
  public Set<String> getMultiTagValues(Set<String> refTags) {
    return refTags
      .stream()
      .map(this::getTag)
      .filter(Objects::nonNull)
      .flatMap(v -> Arrays.stream(v.split(";")))
      .map(String::strip)
      .filter(v -> !v.isBlank())
      .collect(Collectors.toUnmodifiableSet());
  }

  public OsmProvider getOsmProvider() {
    return osmProvider;
  }

  public void setOsmProvider(OsmProvider provider) {
    this.osmProvider = provider;
  }

  /**
   * Determines whether this OSM way is considered routable. The majority of routable ways are those
   * with a highway= tag (which includes everything from motorways to hiking trails). Anything with
   * a public_transport=platform or railway=platform tag is also considered routable even if it
   * doesn't have a highway tag.
   */
  public boolean isRoutable() {
    if (isOneOfTags("highway", NON_ROUTABLE_HIGHWAYS)) {
      return false;
    } else if (hasTag("highway") || isPlatform() || isIndoorRoutable()) {
      if (isGeneralAccessDenied()) {
        // There are exceptions.
        return (
          isMotorcarExplicitlyAllowed() ||
          isBicycleExplicitlyAllowed() ||
          isPedestrianExplicitlyAllowed() ||
          isMotorVehicleExplicitlyAllowed() ||
          isVehicleExplicitlyAllowed()
        );
      }
      return true;
    }

    return false;
  }

  public boolean isIndoorRoutable() {
    return isOneOfTags("indoor", INDOOR_ROUTABLE_VALUES);
  }

  /**
   * Is this a link to another road, like a highway ramp.
   */
  public boolean isLink() {
    String highway = getTag("highway");
    return highway != null && highway.endsWith(("_link"));
  }

  public boolean isElevator() {
    return isTag("highway", "elevator");
  }

  /**
   * @return true if there is no explicit tag that makes this unsuitable for wheelchair use.
   *         In other words: we assume that something is wheelchair-accessible in the absence
   *         of other information.
   */
  public boolean isWheelchairAccessible() {
    return !isTagFalse("wheelchair");
  }

  /**
   * Does this entity have tags that allow extracting a name?
   */
  public boolean isNamed() {
    return hasTag("name") || hasTag("ref");
  }

  /**
   * Is this entity unnamed?
   * <p>
   * Perhaps this entity has a name that isn't in the source data, but it's also possible that
   * it's explicitly tagged as not having one.
   *
   * @see OsmEntity#isExplicitlyUnnamed()
   */
  public boolean hasNoName() {
    return !isNamed();
  }

  /**
   * Whether this entity explicitly doesn't have a name. This is different to no name being
   * set on the entity in OSM.
   *
   * @see OsmEntity#isNamed()
   * @see https://wiki.openstreetmap.org/wiki/Tag:noname%3Dyes
   */
  public boolean isExplicitlyUnnamed() {
    return isTagTrue("noname");
  }

  /**
   * Returns true if this tag is explicitly access to this entity.
   */
  private boolean isTagDeniedAccess(String tagName) {
    String tagValue = getTag(tagName);
    return "no".equals(tagValue) || "license".equals(tagValue);
  }

  /**
   * Returns level tag (i.e. building floor) or layer tag values, defaults to "0"
   * Some entities can have a semicolon separated list of levels (e.g. elevators)
   */
  public Set<String> getLevels() {
    var levels = getMultiTagValues(LEVEL_TAGS);
    if (levels.isEmpty()) {
      // default
      return DEFAULT_LEVEL;
    }
    return levels;
  }

  /**
   * Given an assumed traversal permissions, check if there are explicit additional tags, like bicycle=no
   * or bicycle=yes that override them.
   */
  public StreetTraversalPermission overridePermissions(StreetTraversalPermission def) {
    StreetTraversalPermission permission;

    /*
     * Only a few tags are examined here, because we only care about modes supported by OTP
     * (wheelchairs are not of concern here)
     *
     * Only a few values are checked for, all other values are presumed to be permissive (=>
     * This may not be perfect, but is closer to reality, since most people don't follow the
     * rules perfectly ;-)
     */
    if (isGeneralAccessDenied()) {
      // this can actually be overridden
      permission = StreetTraversalPermission.NONE;
    } else {
      permission = def;
    }

    if (isVehicleExplicitlyDenied()) {
      permission = permission.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
    } else if (isVehicleExplicitlyAllowed()) {
      permission = permission.add(StreetTraversalPermission.BICYCLE_AND_CAR);
    }

    if (isMotorcarExplicitlyDenied() || isMotorVehicleExplicitlyDenied()) {
      permission = permission.remove(StreetTraversalPermission.CAR);
    } else if (isMotorcarExplicitlyAllowed() || isMotorVehicleExplicitlyAllowed()) {
      permission = permission.add(StreetTraversalPermission.CAR);
    }

    if (isBicycleExplicitlyDenied()) {
      permission = permission.remove(StreetTraversalPermission.BICYCLE);
    } else if (isBicycleExplicitlyAllowed()) {
      permission = permission.add(StreetTraversalPermission.BICYCLE);
    }

    if (isPedestrianExplicitlyDenied()) {
      permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
    } else if (isPedestrianExplicitlyAllowed()) {
      permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
    }

    if (isUnderConstruction()) {
      permission = StreetTraversalPermission.NONE;
    }

    if (permission == null) {
      return def;
    }

    /*
     * pedestrian rules: everything is two-way (assuming pedestrians are allowed at all) bicycle
     * rules: default: permissions;
     *
     * cycleway=dismount means walk your bike -- the engine will automatically try walking bikes
     * any time it is forbidden to ride them, so the only thing to do here is to remove bike
     * permissions
     *
     * oneway=... sets permissions for cars and bikes oneway:bicycle overwrites these
     * permissions for bikes only
     *
     * now, cycleway=opposite_lane, opposite, opposite_track can allow once oneway has been set
     * by oneway:bicycle, but should give a warning if it conflicts with oneway:bicycle
     *
     * bicycle:backward=yes works like oneway:bicycle=no bicycle:backwards=no works like
     * oneway:bicycle=yes
     */

    // Compute bike permissions, check consistency.
    if (isBicycleExplicitlyAllowed()) {
      permission = permission.add(StreetTraversalPermission.BICYCLE);
    }

    if (isBicycleDismountForced()) {
      permission = permission.remove(StreetTraversalPermission.BICYCLE);
    }
    return permission;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("tags", tags).toString();
  }
}
