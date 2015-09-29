/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.util;

import java.text.MessageFormat;
import java.util.*;

import gnu.gettext.GettextResource;
import org.opentripplanner.util.i18n.T;
import org.opentripplanner.util.i18n.translations.Units;
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

    /**
     * Specifies which locale uses which distance unit.
     *
     * Currently English - imperial and everything else metric
     *
     * @param locale
     * @return
     */
    public Units getUnits(Locale locale) {
            if (locale.getLanguage().equals("en")) {
                return Units.IMPERIAL;
            } else {
                return Units.METRIC;
            }

    }

    /**
     * Do gettext localization and replace parameters with values from arguments
     * @param msg
     * @param locale
     * @param arguments
     * @return
     */
    public String localizeGettext(T msg, Locale locale, Object[] arguments) {
        String translation = localizeGettext(msg, locale);
        MessageFormat format = new MessageFormat("", locale);
        format.applyPattern(translation);
        return format.format(arguments);
    }

    /**
     * This is temporary function that localizes strings with named sprintf parameters
     *
     * It uses {@link #localizeGettext(T, Locale)} for localization.
     *
     * It is used since current translations all have sprinf named parameters ("%(streetName)s")
     * and java doesn't support them
     *
     * From http://stackoverflow.com/a/2295004
     * @param msg
     * @param locale language of translation
     * @param values map of keys with values that need to be replaced on those places
     * @return
     */
    public String localizeGettextSprintfFormat(T msg, Locale locale, Map<String, String> values) {
        String format = localizeGettext(msg, locale);
        StringBuilder convFormat = new StringBuilder(format);
        Set<String> keys = values.keySet();
        List<String> valueList = new ArrayList<>();
        int currentPos = 1;
        for(String key: keys) {
            String formatKey = "%(" + key + ")",
                formatPos = "%" + Integer.toString(currentPos) + "$";
            int index = -1;
            while ((index = convFormat.indexOf(formatKey, index)) != -1) {
                convFormat.replace(index, index + formatKey.length(), formatPos);
                index += formatPos.length();
            }
            valueList.add(values.get(key));
            ++currentPos;
        }
        return String.format(locale, convFormat.toString(), valueList.toArray());
    }

    public String localizeGettext(T msg, Locale locale) {
        if (msg == null) {
            return null;
        }

        if (locale == null) {
            locale = getDefaultLocale();
        }
        String translation;
        if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            translation = msg.msgid;
        } else {
            try {
                ResourceBundle resourceBundle = null;
                resourceBundle = ResourceBundle
                    .getBundle("org.opentripplanner.util.i18n.translations.Messages", locale);
                    if (msg.msgctx != null) {
                        translation = GettextResource
                            .pgettext(resourceBundle, msg.msgctx, msg.msgid);
                    } else {
                        translation = GettextResource.gettext(resourceBundle, msg.msgid);
                    }
            } catch (MissingResourceException e) {
                LOG.error("Missing resource for key: " + msg, e);
                translation = msg.msgid;
            }
        }
        return translation;
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

    public static String removeHTMLTags(String s) {
        if (s == null) {
            return s;
        }

        return s.replaceAll("\\<.*?>","");
    }
}
