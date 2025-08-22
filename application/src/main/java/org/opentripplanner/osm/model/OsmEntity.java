package org.opentripplanner.osm.model;

import static org.opentripplanner.osm.model.Permission.ALLOW;
import static org.opentripplanner.osm.model.Permission.DENY;
import static org.opentripplanner.osm.model.TraverseDirection.DIRECTIONLESS;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

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

  protected static final Map<String, StreetTraversalPermission> BARRIER_PERMISSIONS = Map.ofEntries(
    // refer to https://wiki.openstreetmap.org/wiki/Key:barrier for meanings
    // if it is not listed, it is assumed to be ALL
    Map.entry("cable_barrier", PEDESTRIAN),
    Map.entry("city_wall", NONE),
    Map.entry("ditch", NONE),
    Map.entry("guard_rail", PEDESTRIAN),
    Map.entry("handrail", PEDESTRIAN),
    Map.entry("hedge", NONE),
    Map.entry("retaining_wall", NONE),
    Map.entry("wall", NONE),
    Map.entry("block", PEDESTRIAN_AND_BICYCLE),
    Map.entry("bollard", PEDESTRIAN_AND_BICYCLE),
    Map.entry("bus_trap", PEDESTRIAN),
    Map.entry("cycle_barrier", PEDESTRIAN),
    Map.entry("debris", PEDESTRIAN),
    Map.entry("full-height_turnstile", PEDESTRIAN),
    Map.entry("horse_stile", PEDESTRIAN),
    Map.entry("kent_carriage_gap", PEDESTRIAN_AND_BICYCLE),
    Map.entry("kissing_gate", PEDESTRIAN),
    Map.entry("motorcycle_barrier", PEDESTRIAN_AND_BICYCLE),
    Map.entry("planter", PEDESTRIAN_AND_BICYCLE),
    Map.entry("stile", PEDESTRIAN),
    Map.entry("sump_buster", PEDESTRIAN_AND_BICYCLE),
    Map.entry("turnstile", PEDESTRIAN),
    Map.entry("wedge", NONE),
    Map.entry("wicket_gate", PEDESTRIAN),
    Map.entry("bar", PEDESTRIAN),
    Map.entry("barrier_board", PEDESTRIAN),
    Map.entry("chain", PEDESTRIAN),
    Map.entry("fence", NONE),
    Map.entry("jersey_barrier", PEDESTRIAN),
    Map.entry("log", PEDESTRIAN),
    Map.entry("rope", PEDESTRIAN),
    Map.entry("tank_trap", PEDESTRIAN),
    Map.entry("tyres", PEDESTRIAN),
    Map.entry("delineator_kerb", PEDESTRIAN_AND_BICYCLE),
    Map.entry("armadillo", PEDESTRIAN_AND_BICYCLE)
  );

  private static final Set<String> WHEELCHAIR_INACCESSIBLE_BARRIERS = Set.of(
    "cable_barrier",
    "city_wall",
    "ditch",
    "guard_rail",
    "handrail",
    "hedge",
    "retaining_wall",
    "wall",
    "block",
    "bus_trap",
    "debris",
    "horse_stile",
    "stile",
    "turnstile",
    "wedge",
    "bar",
    "barrier_board",
    "chain",
    "fence",
    "jersey_barrier",
    "log",
    "tank_trap",
    "tyres"
  );

  private static final Set<String> WHEELCHAIR_ACCESSIBLE_KERBS = Set.of("flush", "lowered", "no");

  private static final Consumer<String> NO_OP = i -> {};

  /**
   * These modes are used to check for permissions for street routing
   */
  protected static final Set<String> CHECKED_MODES = Set.of("foot", "bicycle", "motorcar");

  /**
   * Mapping for the fallback key for checking access restrictions for each access mode in OSM
   * However, access is not included because we are skeptical of access=yes tags.
   */
  private static final Map<String, String> MODE_HIERARACHY = Map.of(
    "motorcar",
    "motor_vehicle",
    "motor_vehicle",
    "vehicle",
    "bicycle",
    "vehicle"
  );
  public static final Set<String> NO_ACCESS_TAGS = Set.of("no", "license", "dismount");
  public static final Map<StreetTraversalPermission, String> OSM_TAGS_FOR_TRAVERSAL_PERMISSION =
    Map.of(
      StreetTraversalPermission.CAR,
      "motorcar",
      StreetTraversalPermission.BICYCLE,
      "bicycle",
      StreetTraversalPermission.PEDESTRIAN,
      "foot"
    );

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
   * Returns the level of wheelchair access of the element explicitly set on the entity.
   */
  public Accessibility explicitWheelchairAccessibility() {
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

  /**
   * Check if there is a permission set for the specified OSM access mode.
   *
   * @param mode The OSM tag of the access mode, such as "motorcar" or "foot"
   * @return an optional value if the mode is allowed or denied access through tagging for the mode
   * or a parent mode, empty if it is not specified.
   */
  protected Optional<Permission> checkModePermission(String mode) {
    return checkModePermission(mode, DIRECTIONLESS);
  }

  /**
   * Check if there is a permission set for the specified OSM access mode in the specified
   * direction.
   *
   * @param mode The OSM tag of the access mode, such as "motorcar" or "foot"
   * @return an optional value if the mode is allowed or denied access through tagging for the mode
   * or a parent mode, either with a directional suffix or not, empty if it is not specified.
   */
  protected Optional<Permission> checkModePermission(String mode, TraverseDirection direction) {
    // check if the exact directional tag allows or denies access
    if (direction != DIRECTIONLESS) {
      if (isExplicitlyAllowed(mode + direction.tagSuffix())) {
        return Optional.of(ALLOW);
      }
      if (isExplicitlyDenied(mode + direction.tagSuffix())) {
        return Optional.of(DENY);
      }
    }
    // check if the exact tag allows or denies access
    if (isExplicitlyAllowed(mode)) {
      return Optional.of(ALLOW);
    }
    if (isExplicitlyDenied(mode)) {
      return Optional.of(DENY);
    }
    // check the parent mode
    var parentMode = MODE_HIERARACHY.get(mode);
    return parentMode == null ? Optional.empty() : checkModePermission(parentMode, direction);
  }

  protected boolean isExplicitlyAllowed(String key) {
    if (tags == null) {
      return false;
    }
    if (isTagTrue(key)) {
      return true;
    }
    key = key.toLowerCase();
    String value = getTag(key);
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

  public Optional<Duration> getDuration(Consumer<String> errorHandler) {
    return getTagValueAsDuration("duration", errorHandler);
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
   *
   * @return true if access is denied in general, regardless if permission is explicitly given
   * when traversing in a specific (forward or backward) direction.
   */
  public boolean isGeneralAccessDenied() {
    return isGeneralAccessDenied(DIRECTIONLESS);
  }

  /**
   * Returns true if access is generally denied to this element (potentially with exceptions)
   * when traversing in the specified direction.
   * Note that oneway tags are not handled in this method.
   */
  public boolean isGeneralAccessDenied(TraverseDirection direction) {
    return checkModePermission("access", direction).map(x -> x == DENY).orElse(false);
  }

  /**
   * Check if the way is one-way w.r.t. to the given traversal mode.
   */
  public Optional<TraverseDirection> isOneWay(@Nullable String mode) {
    var explicitResult = isExplicitlyOneWay(mode);
    if (explicitResult.isPresent()) {
      return explicitResult.get();
    }

    if ("bicycle".equals(mode) && isOpposableCycleway()) {
      return Optional.empty();
    }

    if ("foot".equals(mode) && !isOneOfTags("highway", Set.of("footway", "step", "corridor"))) {
      return Optional.empty();
    }

    if (mode == null) {
      return isTag("highway", "motorway") || isRoundabout()
        ? Optional.of(TraverseDirection.FORWARD)
        : Optional.empty();
    }

    var parentModeResult = isOneWay(MODE_HIERARACHY.get(mode));
    // check if any presence is explicitly overridden, for example, oneway=yes and bicycle:backward=yes
    if (parentModeResult.isPresent()) {
      var direction = parentModeResult.get();
      if (isExplicitlyAllowed(mode + direction.reverse().tagSuffix())) {
        // the way is effectively two-way as the reverse direction is specifically allowed
        return Optional.empty();
      }
    }
    return parentModeResult;
  }

  /**
   * Check if the way is explicitly set as one-way for the specified traversal mode
   * @return empty if it is not explicitly set, value containing empty if it is explicitly set
   * as two-way.
   */
  private Optional<Optional<TraverseDirection>> isExplicitlyOneWay(@Nullable String mode) {
    String key = mode == null ? "oneway" : "oneway:" + mode;
    if (isTagFalse(key)) {
      return Optional.of(Optional.empty());
    }
    if (isTagTrue(key)) {
      return Optional.of(Optional.of(TraverseDirection.FORWARD));
    }
    return isTag(key, "-1")
      ? Optional.of(Optional.of(TraverseDirection.BACKWARD))
      : Optional.empty();
  }

  /**
   * Returns true if bicycles are denied.
   */
  public boolean isBicycleDenied() {
    return checkModePermission("bicycle").equals(Optional.of(DENY));
  }

  /**
   * Returns true if pedestrians are denied.
   */
  public boolean isPedestrianDenied() {
    return checkModePermission("foot").equals(Optional.of(DENY));
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
      ((parkingType != null && parkingType.contains("park_and_ride")) ||
        (parkAndRide != null && !parkAndRide.equalsIgnoreCase("no")))
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
      isTag("highway", "platform") ||
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
      (isTag("railway", "subway_entrance") ||
        isTag("highway", "elevator") ||
        isTag("entrance", "yes") ||
        isTag("entrance", "main")) &&
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

  /**
   * Is this way a roundabout?
   */
  public boolean isRoundabout() {
    return "roundabout".equals(getTag("junction"));
  }

  /**
   * Some cycleways allow contraflow biking.
   */
  public boolean isOpposableCycleway() {
    // any cycleway which is opposite* allows contraflow biking
    String cycleway = getTag("cycleway");
    String cyclewayLeft = getTag("cycleway:left");
    String cyclewayRight = getTag("cycleway:right");

    return (
      (cycleway != null && cycleway.startsWith("opposite")) ||
      (cyclewayLeft != null && cyclewayLeft.startsWith("opposite")) ||
      (cyclewayRight != null && cyclewayRight.startsWith("opposite"))
    );
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
      if (
        isGeneralAccessDenied(DIRECTIONLESS) &&
        isGeneralAccessDenied(TraverseDirection.FORWARD) &&
        isGeneralAccessDenied(TraverseDirection.BACKWARD)
      ) {
        // There are exceptions.
        for (var mode : CHECKED_MODES) {
          for (var direction : TraverseDirection.values()) {
            if (checkModePermission(mode, direction).equals(Optional.of(ALLOW))) {
              return true;
            }
          }
        }
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
    if (isTagTrue("wheelchair")) {
      return true;
    }
    if (isTagFalse("wheelchair")) {
      return false;
    }
    if (isOneOfTags("barrier", WHEELCHAIR_INACCESSIBLE_BARRIERS)) {
      return false;
    }
    if (isTag("barrier", "kerb")) {
      return isOneOfTags("kerb", WHEELCHAIR_ACCESSIBLE_KERBS);
    }
    return true;
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
  private boolean isExplicitlyDenied(String key) {
    return isOneOfTags(key, NO_ACCESS_TAGS);
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

  public StreetTraversalPermission getPermission() {
    return getPermission(DIRECTIONLESS);
  }

  public StreetTraversalPermission getPermission(TraverseDirection direction) {
    return getOsmProvider().getWayPropertySet().getDataForEntity(this, direction).getPermission();
  }

  private StreetTraversalPermission getBarrierPermission() {
    String barrier = getTag("barrier");
    if (barrier == null) {
      return ALL;
    }
    return Objects.requireNonNullElse(BARRIER_PERMISSIONS.get(barrier), ALL);
  }

  /**
   * Given an assumed traversal permissions, check if there are explicit additional tags, like bicycle=no
   * or bicycle=yes that override them.
   */
  public StreetTraversalPermission overridePermissions(StreetTraversalPermission def) {
    return overridePermissions(def, DIRECTIONLESS);
  }

  /**
   * Given an assumed traversal permissions, check if there are explicit additional tags, like bicycle=no
   * or bicycle=yes that override them when traversing in the given direction.
   */
  public StreetTraversalPermission overridePermissions(
    StreetTraversalPermission def,
    TraverseDirection direction
  ) {
    StreetTraversalPermission permission = def;

    permission = permission.intersection(getBarrierPermission());

    if (isGeneralAccessDenied(direction)) {
      permission = StreetTraversalPermission.NONE;
    }

    // handle explicit permissions
    for (var entry : OSM_TAGS_FOR_TRAVERSAL_PERMISSION.entrySet()) {
      var modePermission = checkModePermission(entry.getValue(), direction);
      if (modePermission.isPresent()) {
        permission = switch (modePermission.get()) {
          case ALLOW -> permission.add(entry.getKey());
          case DENY -> permission.remove(entry.getKey());
        };
      }
      if (isOneWay(entry.getValue()).map(wayDirection -> wayDirection != direction).orElse(false)) {
        // cannot travel against one-way road
        permission = permission.remove(entry.getKey());
      }
    }

    if (isUnderConstruction()) {
      permission = StreetTraversalPermission.NONE;
    }

    return permission;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("tags", tags).toString();
  }
}
