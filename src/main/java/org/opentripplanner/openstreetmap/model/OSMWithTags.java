package org.opentripplanner.openstreetmap.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.opentripplanner.graph_builder.module.osm.TemplateLibrary;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;

/**
 * A base class for OSM entities containing common methods.
 */

public class OSMWithTags {

  /* To save memory this is only created when an entity actually has tags. */
  private Map<String, String> tags;

  protected long id;

  protected I18NString creativeName;

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
  public void addTag(OSMTag tag) {
    if (tags == null) tags = new HashMap<>();

    tags.put(tag.getK().toLowerCase(), tag.getV());
  }

  /**
   * Adds a tag.
   */
  public void addTag(String key, String value) {
    if (key == null || value == null) return;

    if (tags == null) tags = new HashMap<>();

    tags.put(key.toLowerCase(), value);
  }

  /**
   * The tags of an entity.
   */
  public Map<String, String> getTags() {
    return tags;
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
   * Determines if a tag contains a true value. 'yes', 'true', and '1' are considered true.
   */
  public boolean isTagTrue(String tag) {
    tag = tag.toLowerCase();
    if (tags == null) {
      return false;
    }

    return isTrue(getTag(tag));
  }

  public boolean doesTagAllowAccess(String tag) {
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
  public String getTag(String tag) {
    tag = tag.toLowerCase();
    if (tags != null && tags.containsKey(tag)) {
      return tags.get(tag);
    }
    return null;
  }

  /**
   * Get tag and convert it to an integer. If the tag exist, but can not be parsed into a number,
   * then the error handler is called with the value witch failed to parse.
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
   * Checks is a tag contains the specified value.
   */
  public Boolean isTag(String tag, String value) {
    tag = tag.toLowerCase();
    if (tags != null && tags.containsKey(tag) && value != null) {
      return value.equals(tags.get(tag));
    }

    return false;
  }

  /**
   * Returns a name-like value for an entity (if one exists). The otp: namespaced tags are created
   * by {@link org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule}
   */
  public I18NString getAssumedName() {
    if (tags == null) {
      return null;
    }
    if (tags.containsKey("name")) {
      return TranslatedString.getI18NString(
        TemplateLibrary.generateI18N("{name}", this),
        true,
        false
      );
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

  public Map<String, String> getTagsByPrefix(String prefix) {
    Map<String, String> out = new HashMap<>();
    for (Map.Entry<String, String> entry : tags.entrySet()) {
      String k = entry.getKey();
      if (k.equals(prefix) || k.startsWith(prefix + ":")) {
        out.put(k, entry.getValue());
      }
    }
    if (out.isEmpty()) {
      return null;
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
   * bicycle is denied if bicycle:no, bicycle:license or bicycle:use_sidepath
   */
  public boolean isBicycleExplicitlyDenied() {
    return isTagDeniedAccess("bicycle") || "use_sidepath".equals(getTag("bicycle"));
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
   * @return True if this node / area is a park and ride.
   */
  public boolean isParkAndRide() {
    String parkingType = getTag("parking");
    String parkAndRide = getTag("park_ride");
    return (
      isTag("amenity", "parking") &&
      (
        (parkingType != null && parkingType.contains("park_and_ride")) ||
        (parkAndRide != null && !parkAndRide.equalsIgnoreCase("no"))
      )
    );
  }

  /**
   * Is this a public transport boarding location where passengers wait for transti and that can be
   * linked to a transit stop vertex later on.
   * <p>
   * This intentionally excludes railway=stop and public_transport=stop because these are supposed
   * to be placed on the tracks not on the platform.
   *
   * @return whether the node is a transit stop
   */
  public boolean isBoardingLocation() {
    return (
      "bus_stop".equals(getTag("highway")) ||
      "tram_stop".equals(getTag("railway")) ||
      "station".equals(getTag("railway")) ||
      "halt".equals(getTag("railway")) ||
      "bus_station".equals(getTag("amenity")) ||
      isPlatform()
    );
  }

  public boolean isPlatform() {
    return "platform".equals(getTag("public_transport")) || "platform".equals(getTag("railway"));
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

  public String getOpenStreetMapLink() {
    return null;
  }

  /**
   * Returns all non-empty values of the tags passed in as input values.
   *
   * Values are split by semicolons.
   *
   */
  public Set<String> getTagValues(Set<String> refTags) {
    return refTags
      .stream()
      .map(this::getTag)
      .filter(Objects::nonNull)
      .flatMap(v -> Arrays.stream(v.split(";")))
      .map(String::strip)
      .filter(v -> !v.isBlank())
      .collect(Collectors.toSet());
  }

  /**
   * Returns true if this tag is explicitly access to this entity.
   */
  private boolean isTagDeniedAccess(String tagName) {
    String tagValue = getTag(tagName);
    return "no".equals(tagValue) || "license".equals(tagValue);
  }
}
