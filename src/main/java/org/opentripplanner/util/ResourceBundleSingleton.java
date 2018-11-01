package org.opentripplanner.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mabu
 */
public enum ResourceBundleSingleton {
    INSTANCE;

    private final static Logger LOG = LoggerFactory.getLogger(ResourceBundleSingleton.class);

    //TODO: this is not the only place default is specified
    //It is also specified in RoutingResource and RoutingRequest
    private final Locale defaultLocale = new Locale("en");

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    //in singleton because resurce bundles are cached based on calling class
    //http://java2go.blogspot.com/2010/03/dont-be-smart-never-implement-resource.html
    public String localize(String key, Locale locale) {
        if (key == null) {
            return null;
        }
        if (locale == null) {
            locale = getDefaultLocale();
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
        Locale locale;
        String[] localeSpecParts = localeSpec.split("_");
        switch (localeSpecParts.length) {
            case 1:
                locale = new Locale(localeSpecParts[0]);
                break;
            case 2:
                locale = new Locale(localeSpecParts[0]);
                break;
            case 3:
                locale = new Locale(localeSpecParts[0]);
                break;
            default:
                LOG.debug("Bogus locale " + localeSpec + ", defaulting to " + defaultLocale.toLanguageTag());
                locale = defaultLocale;
        }
        return locale;
    }
}
