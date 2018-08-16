package org.opentripplanner.openstreetmap.model;

import java.util.HashMap;
import java.util.Map;

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
        if (tags == null)
            tags = new HashMap<String, String>();

        tags.put(tag.getK().toLowerCase(), tag.getV());
    }

    /**
     * Adds a tag.
     */
    public void addTag(String key, String value) {
        if (key == null || value == null)
            return;

        if (tags == null)
            tags = new HashMap<String, String>();

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
        if (tags == null)
            return false;

        return isFalse(getTag(tag));
    }

    /**
     * Determines if a tag contains a true value. 'yes', 'true', and '1' are considered true.
     */
    public boolean isTagTrue(String tag) {
        tag = tag.toLowerCase();
        if (tags == null)
            return false;

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
        return ("designated".equals(value) || "official".equals(value)
                || "permissive".equals(value) || "unknown".equals(value));
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
     * Checks is a tag contains the specified value.
     */
    public Boolean isTag(String tag, String value) {
        tag = tag.toLowerCase();
        if (tags != null && tags.containsKey(tag) && value != null)
            return value.equals(tags.get(tag));

        return false;
    }

    /**
     * Returns a name-like value for an entity (if one exists). The otp: namespaced tags are created by
     * {@link org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule#processRelations processRelations}
     */
    public I18NString getAssumedName() {
        if (tags.containsKey("name"))
            return TranslatedString.getI18NString(TemplateLibrary.generateI18N("{name}", this));

        if (tags.containsKey("otp:route_name"))
            return new NonLocalizedString(tags.get("otp:route_name"));

        if (this.creativeName != null)
            return this.creativeName;

        if (tags.containsKey("otp:route_ref"))
            return new NonLocalizedString(tags.get("otp:route_ref"));

        if (tags.containsKey("ref"))
            return new NonLocalizedString(tags.get("ref"));

        return null;
    }

    public Map<String, String> getTagsByPrefix(String prefix) {
        Map<String, String> out = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            String k = entry.getKey();
            if (k.equals(prefix) || k.startsWith(prefix + ":")) {
                out.put(k, entry.getValue());
            }
        }

        if (out.isEmpty())
            return null;
        return out;
    }

    public static boolean isFalse(String tagValue) {
        return ("no".equals(tagValue) || "0".equals(tagValue) || "false".equals(tagValue));
    }

    public static boolean isTrue(String tagValue) {
        return ("yes".equals(tagValue) || "1".equals(tagValue) || "true".equals(tagValue));
    }

    /**
     * Returns true if this element is under construction.
     * 
     * @return
     */
    public boolean isUnderConstruction() {
        String highway = getTag("highway");
        String cycleway = getTag("cycleway");
        return "construction".equals(highway) || "construction".equals(cycleway);
    }

    /**
     * Returns true if this tag is explicitly access to this entity.
     * 
     * @param tagName
     * @return
     */
    private boolean isTagDeniedAccess(String tagName) {
        String tagValue = getTag(tagName);
        return "no".equals(tagValue) || "license".equals(tagValue);
    }

    /**
     * Returns true if access is generally denied to this element (potentially with exceptions).
     * 
     * @return
     */
    public boolean isGeneralAccessDenied() {
        return isTagDeniedAccess("access");
    }

    /**
     * Returns true if cars are explicitly denied access.
     * 
     * @return
     */
    public boolean isMotorcarExplicitlyDenied() {
        return isTagDeniedAccess("motorcar");
    }

    /**
     * Returns true if cars are explicitly allowed.
     * 
     * @return
     */
    public boolean isMotorcarExplicitlyAllowed() {
        return doesTagAllowAccess("motorcar");
    }

    /**
     * Returns true if cars/motorcycles/HGV are explicitly denied access.
     *
     * @return
     */
    public boolean isMotorVehicleExplicitlyDenied() {
        return isTagDeniedAccess("motor_vehicle");
    }

    /**
     * Returns true if cars/motorcycles/HGV are explicitly allowed.
     *
     * @return
     */
    public boolean isMotorVehicleExplicitlyAllowed() {
        return doesTagAllowAccess("motor_vehicle");
    }


    /**
     * Returns true if bikes are explicitly denied access.
     * 
     * bicycle is denied if bicycle:no, bicycle:license or bicycle:use_sidepath
     * @return
     */
    public boolean isBicycleExplicitlyDenied() {
        return isTagDeniedAccess("bicycle") || "use_sidepath".equals(getTag("bicycle"));
    }

    /**
     * Returns true if bikes are explicitly allowed.
     * 
     * @return
     */
    public boolean isBicycleExplicitlyAllowed() {
        return doesTagAllowAccess("bicycle");
    }

    /**
     * Returns true if pedestrians are explicitly denied access.
     * 
     * @return
     */
    public boolean isPedestrianExplicitlyDenied() {
        return isTagDeniedAccess("foot");
    }

    /**
     * Returns true if pedestrians are explicitly allowed.
     * 
     * @return
     */
    public boolean isPedestrianExplicitlyAllowed() {
        return doesTagAllowAccess("foot");
    }

    /**
     * Returns true if through traffic is not allowed.
     * 
     * @return
     */
    public boolean isThroughTrafficExplicitlyDisallowed() {
        String access = getTag("access");
        boolean noThruTraffic = "destination".equals(access) || "private".equals(access)
                || "customers".equals(access) || "delivery".equals(access)
                || "forestry".equals(access) || "agricultural".equals(access);
        return noThruTraffic;
    }
    
    /**
     * @return True if this node / area is a park and ride.
     */
    public boolean isParkAndRide() {
        String parkingType = getTag("parking");
        String parkAndRide = getTag("park_ride");
        return isTag("amenity", "parking")
                && (parkingType != null && parkingType.contains("park_and_ride"))
                || (parkAndRide != null && !parkAndRide.equalsIgnoreCase("no"));
    }

    /**
     * @return True if this node / area is a bike parking.
     */
    public boolean isBikeParking() {
        return isTag("amenity", "bicycle_parking") && !isTag("access", "private")
                && !isTag("access", "no");
    }

    public void setCreativeName(I18NString creativeName) {
        this.creativeName = creativeName;
    }
}
