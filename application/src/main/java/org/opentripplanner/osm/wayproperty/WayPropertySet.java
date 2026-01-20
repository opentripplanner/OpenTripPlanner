package org.opentripplanner.osm.wayproperty;

import static org.opentripplanner.osm.model.TraverseDirection.BACKWARD;
import static org.opentripplanner.osm.model.TraverseDirection.DIRECTIONLESS;
import static org.opentripplanner.osm.model.TraverseDirection.FORWARD;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.framework.functional.FunctionUtils.TriFunction;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.model.TraverseDirection;
import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.note.StreetNoteAndMatcher;

/**
 * Information given to the GraphBuilder about how to assign permissions, safety values, names, etc.
 * to edges based on OSM tags.
 * <p>
 * WayPropertyPickers, CreativeNamePickers, SlopeOverridePickers, and SpeedPickers are applied to ways based on how well
 * their OSMSpecifiers match a given OSM way. Generally one OSMSpecifier will win out over all the others based on the
 * number of exact, partial, and wildcard tag matches. See OSMSpecifier for more details on the matching process.
 */
public class WayPropertySet {

  /** Sets 1.0 as default safety value for all permissions. */
  public static final TriFunction<
    StreetTraversalPermission,
    Float,
    OsmEntity,
    Double
  > DEFAULT_SAFETY_RESOLVER = ((permission, speedLimit, osmWay) -> 1.0);

  private final List<WayPropertyPicker> wayProperties;

  /** Assign names to ways that do not have them based on OSM tags. */
  private final List<CreativeNamerPicker> creativeNamers;

  private final List<SlopeOverridePicker> slopeOverrides;

  /** Assign automobile speeds based on OSM tags. */
  private final List<SpeedPicker> speedPickers;

  private final List<NotePicker> notes;

  private final List<MixinProperties> mixins;

  /** The automobile speed for street segments that do not match any SpeedPicker. */
  private final Float defaultCarSpeed;

  /**
   * The maximum automobile speed that can be defined through OSM speed limit tagging. Car speed
   * defaults for different way types can be higher than this.
   */
  private final Float maxPossibleCarSpeed;

  /** Resolves walk safety value for each {@link StreetTraversalPermission}. */
  private final TriFunction<
    StreetTraversalPermission,
    Float,
    OsmEntity,
    Double
  > defaultWalkSafetyForPermission;

  /** Resolves bicycle safety value for each {@link StreetTraversalPermission}. */
  private final TriFunction<
    StreetTraversalPermission,
    Float,
    OsmEntity,
    Double
  > defaultBicycleSafetyForPermission;

  /** The WayProperties applied to all ways that do not match any WayPropertyPicker. */
  private final WayProperties defaultProperties;

  WayPropertySet(WayPropertySetBuilder builder) {
    this.wayProperties = List.copyOf(builder.wayProperties);
    this.creativeNamers = List.copyOf(builder.creativeNamers);
    this.slopeOverrides = List.copyOf(builder.slopeOverrides);
    this.speedPickers = List.copyOf(builder.speedPickers);
    this.notes = List.copyOf(builder.notes);
    this.mixins = List.copyOf(builder.mixins);
    this.defaultCarSpeed = builder.defaultCarSpeed;
    this.maxPossibleCarSpeed = builder.maxPossibleCarSpeed;
    this.defaultWalkSafetyForPermission = builder.defaultWalkSafetyForPermission;
    this.defaultBicycleSafetyForPermission = builder.defaultBicycleSafetyForPermission;
    this.defaultProperties = builder.defaultProperties;
  }

  public static WayPropertySetBuilder of() {
    return new WayPropertySetBuilder();
  }

  public Float defaultCarSpeed() {
    return defaultCarSpeed;
  }

  public Float maxPossibleCarSpeed() {
    return maxPossibleCarSpeed;
  }

  /**
   * Applies the WayProperties whose OSMPicker best matches this way. In addition, WayProperties
   * that are mixins will have their safety values applied if they match at all.
   */
  public WayPropertiesPair getDataForWay(OsmWay way) {
    return new WayPropertiesPair(getDataForEntity(way, FORWARD), getDataForEntity(way, BACKWARD));
  }

  /**
   * Get the way properties for an OSM entity without a known traverse direction.
   */
  public WayProperties getDataForEntity(OsmEntity entity) {
    return getDataForEntity(entity, DIRECTIONLESS);
  }

  /**
   * Get the way properties for an OSM entity in the specified traverse direction.
   */
  public WayProperties getDataForEntity(OsmEntity entity, TraverseDirection direction) {
    WayProperties result = defaultProperties;
    int bestScore = 0;
    List<MixinProperties> matchedMixins = new ArrayList<>();
    for (WayPropertyPicker picker : wayProperties) {
      OsmSpecifier specifier = picker.specifier();
      WayProperties wayProperties =
        switch (direction) {
          case DIRECTIONLESS -> picker.properties();
          case FORWARD -> picker.forwardProperties();
          case BACKWARD -> picker.backwardProperties();
        };
      var score = specifier.matchScore(entity, direction);
      if (score > bestScore) {
        result = wayProperties;
        bestScore = score;
      }
    }

    for (var mixin : mixins) {
      var score = mixin.specifier().matchScore(entity, direction);
      if (score > 0) {
        matchedMixins.add(mixin);
      }
    }

    float speed = getCarSpeedForWay(entity, DIRECTIONLESS);

    var permission = entity.overridePermissions(result.getPermission(), direction);

    result = result
      .mutate()
      .withPermission(permission)
      .bicycleSafety(
        result
          .bicycleSafetyOpt()
          .orElseGet(() -> defaultBicycleSafetyForPermission.apply(permission, speed, entity))
      )
      .walkSafety(
        result
          .walkSafetyOpt()
          .orElseGet(() -> defaultWalkSafetyForPermission.apply(permission, speed, entity))
      )
      .build();

    /* apply mixins */
    if (!matchedMixins.isEmpty()) {
      result = applyMixins(result, matchedMixins, direction);
    }
    return result;
  }

  public I18NString getCreativeName(OsmEntity entity) {
    CreativeNamer bestNamer = null;
    int bestScore = 0;
    for (CreativeNamerPicker picker : creativeNamers) {
      OsmSpecifier specifier = picker.specifier();
      CreativeNamer namer = picker.namer();
      int score = specifier.matchScore(entity, DIRECTIONLESS);
      if (score > bestScore) {
        bestNamer = namer;
        bestScore = score;
      }
    }
    if (bestNamer == null) {
      return null;
    }
    return bestNamer.generateCreativeName(entity);
  }

  public float getCarSpeedForWay(OsmEntity way, TraverseDirection direction) {
    return getCarSpeedForWay(way, direction, DataImportIssueStore.NOOP);
  }

  /**
   * Calculate the automobile speed, in meters per second, for this way.
   */
  public float getCarSpeedForWay(
    OsmEntity way,
    TraverseDirection direction,
    DataImportIssueStore issueStore
  ) {
    // first, check for maxspeed tags
    Float speed = null;
    Float currentSpeed;

    if (way.hasTag("maxspeed:motorcar")) {
      speed = SpeedParser.getMetersSecondFromSpeed(way.getTag("maxspeed:motorcar"));
    }

    if (speed == null && direction == FORWARD && way.hasTag("maxspeed:forward")) {
      speed = SpeedParser.getMetersSecondFromSpeed(way.getTag("maxspeed:forward"));
    }

    if (speed == null && direction == BACKWARD && way.hasTag("maxspeed:backward")) {
      speed = SpeedParser.getMetersSecondFromSpeed(way.getTag("maxspeed:backward"));
    }

    if (speed == null && way.hasTag("maxspeed:lanes")) {
      for (String lane : way.getTag("maxspeed:lanes").split("\\|")) {
        currentSpeed = SpeedParser.getMetersSecondFromSpeed(lane);
        // Pick the largest speed from the tag
        // currentSpeed might be null if it was invalid, for instance 10|fast|20
        if (currentSpeed != null && (speed == null || currentSpeed > speed)) {
          speed = currentSpeed;
        }
      }
    }

    if (way.hasTag("maxspeed") && speed == null) {
      speed = SpeedParser.getMetersSecondFromSpeed(way.getTag("maxspeed"));
    }

    if (speed != null) {
      // Too low (less than 5 km/h) or too high speed limit indicates an error in the data,
      // we use default speed limits for the way type in that case.
      // The small epsilon is to account for possible rounding errors.
      if (speed < 1.387 || speed > maxPossibleCarSpeed + 0.0001) {
        var id = way.getId();
        var link = way.url();
        issueStore.add(
          "InvalidCarSpeedLimit",
          "OSM object with id '%s' (%s) has an invalid maxspeed value (%f), that speed will be ignored",
          id,
          link,
          speed
        );
      } else {
        return speed;
      }
    }

    // otherwise, we use the speedPickers

    int bestScore = 0;
    Float bestSpeed = null;
    int score;

    // SpeedPickers are constructed in DefaultOsmTagMapper with an OSM specifier
    // (e.g. highway=motorway) and a default speed for that segment.
    for (SpeedPicker picker : speedPickers) {
      OsmSpecifier specifier = picker.specifier();
      score = specifier.matchScore(way, direction);
      if (score > bestScore) {
        bestScore = score;
        bestSpeed = picker.speed();
      }
    }

    if (bestSpeed != null) {
      return bestSpeed;
    } else {
      return this.defaultCarSpeed;
    }
  }

  public Set<StreetNoteAndMatcher> getNoteForWay(OsmEntity way) {
    HashSet<StreetNoteAndMatcher> out = new HashSet<>();
    for (NotePicker picker : notes) {
      OsmSpecifier specifier = picker.specifier();
      NoteProperties noteProperties = picker.noteProperties();
      if (specifier.matchScore(way, DIRECTIONLESS) > 0) {
        out.add(noteProperties.generateNote(way));
      }
    }
    return out;
  }

  public boolean getSlopeOverride(OsmEntity way) {
    boolean result = false;
    int bestScore = 0;
    for (SlopeOverridePicker picker : slopeOverrides) {
      OsmSpecifier specifier = picker.specifier();
      int score = specifier.matchScore(way, DIRECTIONLESS);
      if (score > bestScore) {
        result = picker.override();
        bestScore = score;
      }
    }
    return result;
  }

  @Override
  public int hashCode() {
    return (
      defaultProperties.hashCode() +
      wayProperties.hashCode() +
      creativeNamers.hashCode() +
      slopeOverrides.hashCode()
    );
  }

  @Override
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

  public List<WayPropertyPicker> listWayProperties() {
    return wayProperties;
  }

  public List<MixinProperties> listMixins() {
    return mixins;
  }

  public List<CreativeNamerPicker> listCreativeNamers() {
    return creativeNamers;
  }

  public List<SlopeOverridePicker> listSlopeOverrides() {
    return slopeOverrides;
  }

  public List<SpeedPicker> listSpeedPickers() {
    return speedPickers;
  }

  public List<NotePicker> listNotes() {
    return notes;
  }

  private WayProperties applyMixins(
    WayProperties result,
    List<MixinProperties> mixins,
    TraverseDirection direction
  ) {
    double bicycle = result.bicycleSafety();
    double walk = result.walkSafety();
    StreetTraversalPermission permission = result.getPermission();
    for (var mixin : mixins) {
      var properties = mixin.getDirectionalProperties(direction);
      bicycle *= properties.bicycleSafety();
      walk *= properties.walkSafety();
      permission = permission
        .add(properties.addedPermission())
        .remove(properties.removedPermission());
    }
    return result
      .mutate()
      .bicycleSafety(bicycle)
      .walkSafety(walk)
      .withPermission(permission)
      .build();
  }
}
