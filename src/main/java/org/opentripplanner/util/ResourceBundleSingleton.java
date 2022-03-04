package org.opentripplanner.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mabu
 */
public enum ResourceBundleSingleton {
    INSTANCE;

    private final static Logger LOG = LoggerFactory.getLogger(ResourceBundleSingleton.class);

    final static ResourceBundle.Control noFallbackControl = Control.getNoFallbackControl(Control.FORMAT_PROPERTIES);

    // The default locale to use if none is provided. This avoids using the system locale, which
    // would lead to nondeterministic results.
    private final static Locale defaultLocale = Locale.ENGLISH;

    //in singleton because resurce bundles are cached based on calling class
    //http://java2go.blogspot.com/2010/03/dont-be-smart-never-implement-resource.html
    public String localize(String key, Locale locale) {
        if (key == null) {
            return null;
        }
        if (locale == null) {
            locale = Locale.ROOT;
        }
        try {
            ResourceBundle resourceBundle = null;
            if (key.equals("corner") || key.equals("unnamedStreet") || key.equals("origin") || key.equals("destination")) {
                resourceBundle = ResourceBundle.getBundle("internals", locale, noFallbackControl);
            } else {
                resourceBundle = ResourceBundle.getBundle("WayProperties", locale, noFallbackControl);
            }
            String retval = resourceBundle.getString(key);
            //LOG.debug(String.format("Localized '%s' using '%s'", key, retval));
            return retval;
        } catch (MissingResourceException e) {
            //LOG.debug("Missing translation for key: " + key);
            return key;
        }
    }
    
    /**
     * Gets {@link Locale} from string. Expects en_US, en_GB, de etc.
     *
     * If no valid locale was found defaultLocale (en) is returned.
     * @param localeSpec String which should be locale (en_US, en_GB, de etc.)
     * @return Locale specified with localeSpec
     */
    public Locale getLocale(String localeSpec) {
        if (localeSpec == null || localeSpec.isEmpty()) {
            return defaultLocale;
        }
        //TODO: This should probably use Locale.forLanguageTag
        //but format is little (IETF language tag) different - instead of _
        String[] localeSpecParts = localeSpec.split("_");
        switch (localeSpecParts.length) {
            case 1:
                return new Locale(localeSpecParts[0]);
            case 2:
                return new Locale(localeSpecParts[0]);
            case 3:
                return new Locale(localeSpecParts[0]);
            default:
                LOG.debug("Bogus locale " + localeSpec + ", using default");
                return defaultLocale;
        }
    }
}
