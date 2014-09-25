package org.opentripplanner.osm;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

public abstract class Tagged implements Serializable {

    private static final long serialVersionUID = 1L;
    
    // Format: key1=val1;key2=val2
    public String tags;

    public String getTag(String key) {
        if (tags == null) return null;
        for (String tag : tags.split(";")) {
            String[] kv = tag.split("=");
            if (kv.length == 2) {
                if (kv[0].equals(key)) return kv[1];
            }
            // handle case where value is missing
        }
        return null;
    }
    
    public boolean hasTag(String key) {
        return (getTag(key) != null);
    }
    
    public boolean tagless() {
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

    public static class Tag {
        String key, value;
    }

}
