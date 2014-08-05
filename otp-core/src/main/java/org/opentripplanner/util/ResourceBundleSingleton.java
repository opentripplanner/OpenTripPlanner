/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 *
 * @author mabu
 */
public enum ResourceBundleSingleton {
    INSTANCE;
    //in singleton because resurce bundles are cached based on calling class
    //http://java2go.blogspot.com/2010/03/dont-be-smart-never-implement-resource.html
    public String localize(String key, Locale locale) {
        if (key == null) {
            return null;
        }
        try {
            ResourceBundle resourceBundle = null;
            if (key.equals("corner") || key.equals("unnamedStreet")) {
                resourceBundle = ResourceBundle.getBundle("internals", locale);
            } else {
                resourceBundle = ResourceBundle.getBundle("WayProperties", locale);
            }
            String retval = resourceBundle.getString(key);
            //LOG.debug(String.format("Localized '%s' using '%s'", key, retval));
            return retval;
        } catch (MissingResourceException e) {
            //LOG.debug("Missing translation for key: " + key);
            return key;
        }
    }
    
}
