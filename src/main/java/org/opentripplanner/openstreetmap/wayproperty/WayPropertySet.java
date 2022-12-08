package org.opentripplanner.openstreetmap.wayproperty;

import static org.opentripplanner.openstreetmap.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.specifier.BestMatchSpecifier;
import org.opentripplanner.openstreetmap.wayproperty.specifier.OsmSpecifier;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.note.StreetNoteAndMatcher;
import org.opentripplanner.street.model.note.StreetNoteMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Information given to the GraphBuilder about how to assign permissions, safety values, names, etc.
 * to edges based on OSM tags.
 * TODO rename so that the connection with OSM tags is obvious
 * <p>
 * WayPropertyPickers, CreativeNamePickers, SlopeOverridePickers, and SpeedPickers are applied to ways based on how well
 * their OSMSpecifiers match a given OSM way. Generally one OSMSpecifier will win out over all the others based on the
 * number of exact, partial, and wildcard tag matches. See OSMSpecifier for more details on the matching process.
 */
public class WayPropertySet {

  private static final Logger LOG = LoggerFactory.getLogger(WayPropertySet.class);

  /** Sets 1.0 as default safety value for all permissions. */
  private final BiFunction<StreetTraversalPermission, Float, Double> DEFAULT_SAFETY_RESOLVER =
    ((permission, speedLimit) -> 1.0);

  private final List<WayPropertyPicker> wayProperties;

  /** Assign names to ways that do not have them based on OSM tags. */
  private final List<CreativeNamerPicker> creativeNamers;

  private final List<SlopeOverridePicker> slopeOverrides;

  /** Assign automobile speeds based on OSM tags. */
  private final List<SpeedPicker> speedPickers;
  private final List<NotePicker> notes;
  private final Pattern maxSpeedPattern;
  /** The automobile speed for street segments that do not match any SpeedPicker. */
  public Float defaultSpeed;
  /** Resolves walk safety value for each {@link StreetTraversalPermission}. */
  private BiFunction<StreetTraversalPermission, Float, Double> defaultWalkSafetyForPermission;
  /** Resolves bicycle safety value for each {@link StreetTraversalPermission}. */
  private BiFunction<StreetTraversalPermission, Float, Double> defaultBicycleSafetyForPermission;
  /** The WayProperties applied to all ways that do not match any WayPropertyPicker. */
  private final WayProperties defaultProperties;

  public List<MixinProperties> getMixins() {
    return mixins;
  }

  private final List<MixinProperties> mixins = new ArrayList<>();

  public WayPropertySet() {
    /* sensible defaults */
    defaultSpeed = 11.2f; // 11.2 m/s ~= 25 mph ~= 40 kph, standard speed limit in the US
    defaultProperties = withModes(ALL).build();
    wayProperties = new ArrayList<>();
    creativeNamers = new ArrayList<>();
    slopeOverrides = new ArrayList<>();
    speedPickers = new ArrayList<>();
    notes = new ArrayList<>();
    // regex courtesy http://wiki.openstreetmap.org/wiki/Key:maxspeed
    // and edited
    maxSpeedPattern = Pattern.compile("^([0-9][.0-9]*)\\s*(kmh|km/h|kmph|kph|mph|knots)?$");
    defaultWalkSafetyForPermission = DEFAULT_SAFETY_RESOLVER;
    defaultBicycleSafetyForPermission = DEFAULT_SAFETY_RESOLVER;
  }

  /**
   * Applies the WayProperties whose OSMPicker best matches this way. In addition, WayProperties
   * that are mixins will have their safety values applied if they match at all.
   */
  public WayProperties getDataForWay(OSMWithTags way) {
    WayProperties leftResult = defaultProperties;
    WayProperties rightResult = defaultProperties;
    int bestLeftScore = 0;
    int bestRightScore = 0;
    List<MixinProperties> leftMixins = new ArrayList<>();
    List<MixinProperties> rightMixins = new ArrayList<>();
    for (WayPropertyPicker picker : wayProperties) {
      OsmSpecifier specifier = picker.specifier();
      WayProperties wayProperties = picker.properties();
      var score = specifier.matchScores(way);
      if (score.left() > bestLeftScore) {
        leftResult = wayProperties;
        bestLeftScore = score.left();
      }
      if (score.right() > bestRightScore) {
        rightResult = wayProperties;
        bestRightScore = score.right();
      }
    }

    for (var mixin : mixins) {
      var score = mixin.specifier().matchScores(way);
      if (score.left() > 0) {
        leftMixins.add(mixin);
      }
      if (score.right() > 0) {
        rightMixins.add(mixin);
      }
    }

    float forwardSpeed = getCarSpeedForWay(way, false);
    float backSpeed = getCarSpeedForWay(way, true);
    StreetTraversalPermission permission = rightResult.getPermission();

    WayProperties result = rightResult
      .mutate()
      .bicycleSafety(
        rightResult.getBicycleSafetyFeatures() != null
          ? rightResult.getBicycleSafetyFeatures().forward()
          : defaultBicycleSafetyForPermission.apply(permission, forwardSpeed),
        leftResult.getBicycleSafetyFeatures() != null
          ? leftResult.getBicycleSafetyFeatures().back()
          : defaultBicycleSafetyForPermission.apply(permission, backSpeed)
      )
      .walkSafety(
        rightResult.getWalkSafetyFeatures() != null
          ? rightResult.getWalkSafetyFeatures().forward()
          : defaultWalkSafetyForPermission.apply(permission, forwardSpeed),
        leftResult.getWalkSafetyFeatures() != null
          ? leftResult.getWalkSafetyFeatures().back()
          : defaultWalkSafetyForPermission.apply(permission, backSpeed)
      )
      .build();

    /* apply mixins */
    if (leftMixins.size() > 0) {
      result = applyMixins(result, leftMixins, false);
    }
    if (rightMixins.size() > 0) {
      result = applyMixins(result, rightMixins, true);
    }
    if (
      (bestLeftScore == 0 || bestRightScore == 0) &&
      (leftMixins.size() == 0 || rightMixins.size() == 0)
    ) {
      String all_tags = dumpTags(way);
      LOG.debug("Used default permissions: {}", all_tags);
    }
    return result;
  }

  public I18NString getCreativeNameForWay(OSMWithTags way) {
    CreativeNamer bestNamer = null;
    int bestScore = 0;
    for (CreativeNamerPicker picker : creativeNamers) {
      OsmSpecifier specifier = picker.specifier;
      CreativeNamer namer = picker.namer;
      int score = specifier.matchScore(way);
      if (score > bestScore) {
        bestNamer = namer;
        bestScore = score;
      }
    }
    if (bestNamer == null) {
      return null;
    }
    return bestNamer.generateCreativeName(way);
  }

  /**
   * Calculate the automobile speed, in meters per second, for this way.
   */
  public float getCarSpeedForWay(OSMWithTags way, boolean back) {
    // first, check for maxspeed tags
    Float speed = null;
    Float currentSpeed;

    if (way.hasTag("maxspeed:motorcar")) speed =
      getMetersSecondFromSpeed(way.getTag("maxspeed:motorcar"));

    if (speed == null && !back && way.hasTag("maxspeed:forward")) speed =
      getMetersSecondFromSpeed(way.getTag("maxspeed:forward"));

    if (speed == null && back && way.hasTag("maxspeed:reverse")) speed =
      getMetersSecondFromSpeed(way.getTag("maxspeed:reverse"));

    if (speed == null && way.hasTag("maxspeed:lanes")) {
      for (String lane : way.getTag("maxspeed:lanes").split("\\|")) {
        currentSpeed = getMetersSecondFromSpeed(lane);
        // Pick the largest speed from the tag
        // currentSpeed might be null if it was invalid, for instance 10|fast|20
        if (currentSpeed != null && (speed == null || currentSpeed > speed)) speed = currentSpeed;
      }
    }

    if (way.hasTag("maxspeed") && speed == null) speed =
      getMetersSecondFromSpeed(way.getTag("maxspeed"));

    // this would be bad, as the segment could never be traversed by an automobile
    // The small epsilon is to account for possible rounding errors
    if (speed != null && speed < 0.0001) LOG.warn(
      "Zero or negative automobile speed detected at {} based on OSM " +
      "maxspeed tags; ignoring these tags",
      this
    );

    // if there was a defined speed and it's not 0, we're done
    if (speed != null && speed > 0.0001) return speed;

    // otherwise, we use the speedPickers

    int bestScore = 0;
    Float bestSpeed = null;
    int score;

    // SpeedPickers are constructed in DefaultOsmTagMapper with an OSM specifier
    // (e.g. highway=motorway) and a default speed for that segment.
    for (SpeedPicker picker : speedPickers) {
      OsmSpecifier specifier = picker.specifier;
      score = specifier.matchScore(way);
      if (score > bestScore) {
        bestScore = score;
        bestSpeed = picker.speed;
      }
    }

    if (bestSpeed != null) {
      return bestSpeed;
    } else {
      return this.defaultSpeed;
    }
  }

  public Set<StreetNoteAndMatcher> getNoteForWay(OSMWithTags way) {
    HashSet<StreetNoteAndMatcher> out = new HashSet<>();
    for (NotePicker picker : notes) {
      OsmSpecifier specifier = picker.specifier;
      NoteProperties noteProperties = picker.noteProperties;
      if (specifier.matchScore(way) > 0) {
        out.add(noteProperties.generateNote(way));
      }
    }
    if (out.size() == 0) {
      return null;
    }
    return out;
  }

  public boolean getSlopeOverride(OSMWithTags way) {
    boolean result = false;
    int bestScore = 0;
    for (SlopeOverridePicker picker : slopeOverrides) {
      OsmSpecifier specifier = picker.getSpecifier();
      int score = specifier.matchScore(way);
      if (score > bestScore) {
        result = picker.getOverride();
        bestScore = score;
      }
    }
    return result;
  }

  public void addMixin(MixinProperties mixin) {
    mixins.add(mixin);
  }

  public void addProperties(OsmSpecifier spec, WayProperties properties) {
    wayProperties.add(new WayPropertyPicker(spec, properties));
  }

  public void addCreativeNamer(OsmSpecifier spec, CreativeNamer namer) {
    creativeNamers.add(new CreativeNamerPicker(spec, namer));
  }

  public void addNote(OsmSpecifier osmSpecifier, NoteProperties properties) {
    notes.add(new NotePicker(osmSpecifier, properties));
  }

  public void setSlopeOverride(OsmSpecifier spec, boolean override) {
    slopeOverrides.add(new SlopeOverridePicker(spec, override));
  }

  public int hashCode() {
    return (
      defaultProperties.hashCode() +
      wayProperties.hashCode() +
      creativeNamers.hashCode() +
      slopeOverrides.hashCode()
    );
  }

  public boolean equals(Object o) {
    if (o instanceof WayPropertySet other) {
      return (
        defaultProperties.equals(other.defaultProperties) &&
        wayProperties.equals(other.wayProperties) &&
        creativeNamers.equals(other.creativeNamers) &&
        slopeOverrides.equals(other.slopeOverrides) &&
        notes.equals(other.notes)
      );
    }
    return false;
  }

  public void addSpeedPicker(SpeedPicker picker) {
    this.speedPickers.add(picker);
  }

  public Float getMetersSecondFromSpeed(String speed) {
    Matcher m = maxSpeedPattern.matcher(speed.trim());
    if (!m.matches()) {
      return null;
    }

    float originalUnits;
    try {
      originalUnits = (float) Double.parseDouble(m.group(1));
    } catch (NumberFormatException e) {
      LOG.warn("Could not parse max speed {}", m.group(1));
      return null;
    }

    String units = m.group(2);
    if (units == null || units.equals("")) units = "kmh";

    // we'll be doing quite a few string comparisons here
    units = units.intern();

    float metersSecond;

    switch (units) {
      case "kmh":
      case "km/h":
      case "kmph":
      case "kph":
        metersSecond = 0.277778f * originalUnits;
        break;
      case "mph":
        metersSecond = 0.446944f * originalUnits;
        break;
      case "knots":
        metersSecond = 0.514444f * originalUnits;
        break;
      default:
        return null;
    }

    return metersSecond;
  }

  public void createNames(String spec, String patternKey) {
    CreativeNamer namer = new CreativeNamer(patternKey);
    addCreativeNamer(new BestMatchSpecifier(spec), namer);
  }

  public void createNotes(String spec, String patternKey, StreetNoteMatcher matcher) {
    // TODO: notes aren't localized
    NoteProperties properties = new NoteProperties(patternKey, matcher);
    addNote(new BestMatchSpecifier(spec), properties);
  }

  /**
   * A custom defaultWalkSafetyForPermission can only be set once. The given function should
   * provide a default for each permission. Safety can vary based on car speed limit on a way.
   */
  public void setDefaultWalkSafetyForPermission(
    BiFunction<StreetTraversalPermission, Float, Double> defaultWalkSafetyForPermission
  ) {
    if (!this.defaultWalkSafetyForPermission.equals(DEFAULT_SAFETY_RESOLVER)) {
      throw new IllegalStateException("A custom default walk safety resolver was already set");
    }
    this.defaultWalkSafetyForPermission = defaultWalkSafetyForPermission;
  }

  /**
   * A custom defaultBicycleSafetyForPermission can only be set once. The given function should
   * provide a default for each permission. Safety can vary based on car speed limit on a way.
   */
  public void setDefaultBicycleSafetyForPermission(
    BiFunction<StreetTraversalPermission, Float, Double> defaultBicycleSafetyForPermission
  ) {
    if (!this.defaultBicycleSafetyForPermission.equals(DEFAULT_SAFETY_RESOLVER)) {
      throw new IllegalStateException("A custom default cycling safety resolver was already set");
    }
    this.defaultBicycleSafetyForPermission = defaultBicycleSafetyForPermission;
  }

  public void setMixinProperties(OsmSpecifier spec, MixinPropertiesBuilder builder) {
    addMixin(builder.build(spec));
  }

  public void setMixinProperties(String spec, MixinPropertiesBuilder builder) {
    setMixinProperties(new BestMatchSpecifier(spec), builder);
  }

  public void setProperties(String s, WayProperties props) {
    setProperties(new BestMatchSpecifier(s), props);
  }

  public void setProperties(String spec, WayPropertiesBuilder properties) {
    setProperties(new BestMatchSpecifier(spec), properties);
  }

  public void setProperties(OsmSpecifier spec, WayProperties properties) {
    addProperties(spec, properties);
  }

  public void setProperties(OsmSpecifier spec, WayPropertiesBuilder properties) {
    addProperties(spec, properties.build());
  }

  public void setCarSpeed(String spec, float speed) {
    SpeedPicker picker = new SpeedPicker();
    picker.specifier = new BestMatchSpecifier(spec);
    picker.speed = speed;
    addSpeedPicker(picker);
  }

  public List<WayPropertyPicker> getWayProperties() {
    return Collections.unmodifiableList(wayProperties);
  }

  private String dumpTags(OSMWithTags way) {
    /* generate warning message */
    String all_tags = null;
    Map<String, String> tags = way.getTags();
    for (Entry<String, String> entry : tags.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      String tag = key + "=" + value;
      if (all_tags == null) {
        all_tags = tag;
      } else {
        all_tags += "; " + tag;
      }
    }
    return all_tags;
  }

  private WayProperties applyMixins(
    WayProperties result,
    List<MixinProperties> mixins,
    boolean right
  ) {
    SafetyFeatures bicycleSafetyFeatures = result.getBicycleSafetyFeatures();
    double forwardBicycle = bicycleSafetyFeatures.forward();
    double backBicycle = bicycleSafetyFeatures.back();
    SafetyFeatures walkSafetyFeatures = result.getWalkSafetyFeatures();
    double forwardWalk = walkSafetyFeatures.forward();
    double backWalk = walkSafetyFeatures.back();
    for (var mixin : mixins) {
      if (right) {
        if (mixin.bicycleSafety() != null) {
          backBicycle *= mixin.bicycleSafety().back();
        }
        if (mixin.walkSafety() != null) {
          backWalk *= mixin.walkSafety().back();
        }
      } else {
        if (mixin.bicycleSafety() != null) {
          forwardBicycle *= mixin.bicycleSafety().forward();
        }
        if (mixin.walkSafety() != null) {
          forwardWalk *= mixin.walkSafety().forward();
        }
      }
    }
    return result
      .mutate()
      .bicycleSafety(forwardBicycle, backBicycle)
      .walkSafety(forwardWalk, backWalk)
      .build();
  }
}
