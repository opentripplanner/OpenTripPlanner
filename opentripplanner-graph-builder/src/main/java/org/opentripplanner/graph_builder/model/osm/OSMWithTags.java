/* 
 Copyright 2008 Brian Ferris
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.model.osm;

import java.util.HashMap;
import java.util.Map;

/**
 * A base class for OSM entities containing common methods.
 */

public class OSMWithTags {

    /* To save memory this is only created when an entity actually has tags. */
    private Map<String, String> _tags;

    protected long id;

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
        if (_tags == null)
            _tags = new HashMap<String, String>();

        _tags.put(tag.getK().toLowerCase(), tag.getV());
    }

    /**
     * Adds a tag.
     */
    public void addTag(String key, String value) {
        if (key == null || value == null)
            return;

        if (_tags == null)
            _tags = new HashMap<String, String>();

        _tags.put(key.toLowerCase(), value);
    }

    /**
     * The tags of an entity.
     */
    public Map<String, String> getTags() {
        return _tags;
    }

    /**
     * Is the tag defined?
     */
    public boolean hasTag(String tag) {
        tag = tag.toLowerCase();
        return _tags != null && _tags.containsKey(tag);
    }

    /**
     * Determines if a tag contains a false value. 'no', 'false', and '0' are considered false.
     */
    public boolean isTagFalse(String tag) {
        tag = tag.toLowerCase();
        if (_tags == null)
            return false;

        return ("no".equals(getTag(tag)) || "0".equals(getTag(tag)) || "false".equals(getTag(tag)));
    }

    /**
     * Determines if a tag contains a true value. 'yes', 'true', and '1' are considered true.
     */
    public boolean isTagTrue(String tag) {
        tag = tag.toLowerCase();
        if (_tags == null)
            return false;

        return ("yes".equals(getTag(tag)) || "1".equals(getTag(tag)) || "true".equals(getTag(tag)));
    }

    public boolean doesTagAllowAccess(String tag) {
        if (_tags == null) {
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

    /**
     * Gets a tag's value.
     */
    public String getTag(String tag) {
        tag = tag.toLowerCase();
        if (_tags != null && _tags.containsKey(tag))
            return _tags.get(tag);

        return null;
    }

    /**
     * Checks is a tag contains the specified value.
     */
    public Boolean isTag(String tag, String value) {
        tag = tag.toLowerCase();
        if (_tags != null && _tags.containsKey(tag) && value != null)
            return value.equals(_tags.get(tag));

        return false;
    }

    /**
     * Returns a name-like value for an entity (if one exists). The otp: namespaced tags are created
     * by
     * {@link org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl#processRelations
     * processRelations}
     */
    public String getAssumedName() {
        if (_tags.containsKey("name"))
            return _tags.get("name");

        if (_tags.containsKey("otp:route_name"))
            return _tags.get("otp:route_name");

        if (_tags.containsKey("otp:gen_name"))
            return _tags.get("otp:gen_name");

        if (_tags.containsKey("otp:route_ref"))
            return _tags.get("otp:route_ref");

        if (_tags.containsKey("ref"))
            return _tags.get("ref");

        return null;
    }

    public Map<String, String> getTagsByPrefix(String prefix) {
        Map<String, String> out = new HashMap<String, String>();
       for (Map.Entry<String, String> entry : _tags.entrySet()) {
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
}
