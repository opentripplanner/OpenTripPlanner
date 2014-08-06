/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 *
 * @author mabu
 */
public class LocalizedString implements I18NString, Serializable {
    private static final Pattern pattern = Pattern.compile("\\{(.*?)\\}");
    private static final Matcher matcher = pattern.matcher("");
    private transient static Map<String, String> key_params;
    
    static {
        key_params = new HashMap<String, String>();
    }
    
    private String key;
    private String[] params;

    public LocalizedString(String key, String[] params) {
        this.key = key;
        this.params = params;
    }
    
    //Gets params from translated key
    public LocalizedString(String key, OSMWithTags way) {
        this.key = key;
        List<String> lparams = new ArrayList<String>(1);
        //Which tags do we want from way
        String tag_name = getTagNames();
        if (tag_name != null) {
            //Tag value
            String param = way.getTag(tag_name);
            if (param != null) {
                lparams.add(param);
                this.params = lparams.toArray(new String[lparams.size()]);
            }
        }
    }
    
    /**
     * Finds wanted tag names in name For example "Platform {ref}" ref is way tagname
     * @return 
     */
    private String getTagNames() {
        if( key_params.containsKey(key)) {
            return key_params.get(key);
        }
        String english_trans = ResourceBundleSingleton.INSTANCE.localize(this.key, Locale.ENGLISH);
        matcher.reset(english_trans);
        int lastEnd = 0;
        while (matcher.find()) {
            
            lastEnd = matcher.end();
            // and then the value for the match
            String m_key = matcher.group(1);
            key_params.put(key, m_key);
            return m_key;
        }
        return null;
    }

    @Override
    public String toString() {
        return this.toString(Locale.getDefault());
    }    

    @Override
    public String toString(Locale locale) {
        if (this.key == null) {
            return null;
        }
        //replaces {name}, {ref} etc with %s to be used as parameters
        //in string formatting with values from way tags values
        String translation = ResourceBundleSingleton.INSTANCE.localize(this.key, locale);
        if (this.params != null) {
            translation = pattern.matcher(translation).replaceFirst("%s");
        }
        return String.format(translation, (Object[]) params);
    }

}
