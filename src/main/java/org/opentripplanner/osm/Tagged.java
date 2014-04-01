package org.opentripplanner.osm;

import java.io.Serializable;

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
    
}
