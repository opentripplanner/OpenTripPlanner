package org.opentripplanner.osm;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

public abstract class Tagged implements Serializable {

    private static final long serialVersionUID = 1L;

    // Format: key1=val1;key2=val2
    public String tags;

    public static class Tag {
        String key, value;
    }

    /** Return the tag value for the given key. Returns null if the tag key is not present. */
    public String getTag(String key) {
        if (tags == null) return null;
        for (String tag : tags.split(";")) {
            String[] kv = tag.split("=", 2);
            if (kv[0].equals(key)) {
                if (kv.length == 2) {
                    return kv[1];
                } else {
                    return ""; // key is present but has no value
                }
            }
        }
        return null;
    }
    
    public boolean hasTag(String key) {
        return (getTag(key) != null);
    }

    public boolean hasTag(String key, String value) {
        return (value.equals(getTag(key)));
    }

    public boolean hasNoTags() {
        return tags == null || tags.isEmpty();
    }

    public List<Tag> getTags() {
        List<Tag> ret = Lists.newArrayList();
        if (tags != null) {
            for (String tag : tags.split(";")) {
                String[] kv = tag.split("=");
                if (kv.length < 1) continue;
                Tag t = new Tag();
                t.key = kv[0];
                if (kv.length > 1) t.value = kv[1];
                ret.add(t);
            }
        }
        return ret;
    }

    public boolean tagIsTrue (String key) {
        String value = getTag(key);
        return value != null && ("yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value));
    }

    public boolean tagIsFalse (String key) {
        String value = getTag(key);
        return value != null && ("no".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "0".equals(value));
    }

}
