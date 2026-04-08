package org.opentripplanner.osm.wayproperty;

import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.functional.FunctionUtils;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.wayproperty.specifier.BestMatchSpecifier;
import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.note.StreetNoteMatcher;

public class WayPropertySetBuilder {

  Float defaultCarSpeed = 11.2f;
  Float maxPossibleCarSpeed = 38f;
  WayProperties defaultProperties = withModes(ALL).build();
  final List<WayPropertyPicker> wayProperties = new ArrayList<>();
  final List<CreativeNamerPicker> creativeNamers = new ArrayList<>();
  final List<SlopeOverridePicker> slopeOverrides = new ArrayList<>();
  final List<SpeedPicker> speedPickers = new ArrayList<>();
  final List<NotePicker> notes = new ArrayList<>();
  final List<MixinProperties> mixins = new ArrayList<>();
  FunctionUtils.TriFunction<
    StreetTraversalPermission,
    Float,
    OsmEntity,
    Double
  > defaultWalkSafetyForPermission = WayPropertySet.DEFAULT_SAFETY_RESOLVER;
  FunctionUtils.TriFunction<
    StreetTraversalPermission,
    Float,
    OsmEntity,
    Double
  > defaultBicycleSafetyForPermission = WayPropertySet.DEFAULT_SAFETY_RESOLVER;

  WayPropertySetBuilder() {}

  public void addMixin(MixinProperties mixin) {
    mixins.add(mixin);
  }

  public void addProperties(OsmSpecifier spec, WayProperties properties) {
    addProperties(spec, properties, properties, properties);
  }

  public void addProperties(
    OsmSpecifier spec,
    WayProperties properties,
    WayProperties forwardProperties,
    WayProperties backwardProperties
  ) {
    wayProperties.add(
      new WayPropertyPicker(spec, properties, forwardProperties, backwardProperties)
    );
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

  public void addSpeedPicker(SpeedPicker picker) {
    this.speedPickers.add(picker);
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

  public void setDefaultWalkSafetyForPermission(
    FunctionUtils.TriFunction<
      StreetTraversalPermission,
      Float,
      OsmEntity,
      Double
    > defaultWalkSafetyForPermission
  ) {
    if (!this.defaultWalkSafetyForPermission.equals(WayPropertySet.DEFAULT_SAFETY_RESOLVER)) {
      throw new IllegalStateException("A custom default walk safety resolver was already set");
    }
    this.defaultWalkSafetyForPermission = defaultWalkSafetyForPermission;
  }

  public void setDefaultBicycleSafetyForPermission(
    FunctionUtils.TriFunction<
      StreetTraversalPermission,
      Float,
      OsmEntity,
      Double
    > defaultBicycleSafetyForPermission
  ) {
    if (!this.defaultBicycleSafetyForPermission.equals(WayPropertySet.DEFAULT_SAFETY_RESOLVER)) {
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

  public void setProperties(
    String spec,
    WayPropertiesBuilder properties,
    WayPropertiesBuilder forwardProperties,
    WayPropertiesBuilder backwardProperties
  ) {
    setProperties(new BestMatchSpecifier(spec), properties, forwardProperties, backwardProperties);
  }

  public void setProperties(OsmSpecifier spec, WayProperties properties) {
    addProperties(spec, properties);
  }

  public void setProperties(OsmSpecifier spec, WayPropertiesBuilder properties) {
    addProperties(spec, properties.build());
  }

  public void setProperties(
    OsmSpecifier spec,
    WayPropertiesBuilder properties,
    WayPropertiesBuilder forwardProperties,
    WayPropertiesBuilder backwardProperties
  ) {
    addProperties(spec, properties.build(), forwardProperties.build(), backwardProperties.build());
  }

  public void setCarSpeed(String spec, float speed) {
    addSpeedPicker(new SpeedPicker(new BestMatchSpecifier(spec), speed));
  }

  public void setCarSpeed(OsmSpecifier spec, float speed) {
    addSpeedPicker(new SpeedPicker(spec, speed));
  }

  public void setDefaultCarSpeed(Float defaultCarSpeed) {
    this.defaultCarSpeed = defaultCarSpeed;
  }

  public void setMaxPossibleCarSpeed(Float maxPossibleCarSpeed) {
    this.maxPossibleCarSpeed = maxPossibleCarSpeed;
  }

  /**
   * Takes another way property set and adds its pickers to this builder.
   * Note: It does not add the max car speed, default car speed or the function for the default
   * safety resolver.
   */
  public WayPropertySetBuilder addPickers(WayPropertySet other) {
    this.wayProperties.addAll(other.listWayProperties());
    this.creativeNamers.addAll(other.listCreativeNamers());
    this.slopeOverrides.addAll(other.listSlopeOverrides());
    this.speedPickers.addAll(other.listSpeedPickers());
    this.notes.addAll(other.listNotes());
    this.mixins.addAll(other.listMixins());
    return this;
  }

  public WayPropertySet build() {
    return new WayPropertySet(this);
  }
}
