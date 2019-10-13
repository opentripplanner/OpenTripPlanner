package org.opentripplanner.util;

import java.util.Locale;

/**
 * This interface is used when providing translations on server side.
 * Sources: OSM tags with multiple languages (like street names), or TranslatedString fields in GTFS-RT Alert messages.
 * @author mabu
 */
public interface I18NString {

    /**
     * Returns default translation (english)
     * @return 
     */
    public String toString();
    
    /**
     * Returns wanted translation
     * @param locale Wanted locale
     * @return 
     */
    public String toString(Locale locale);
    
}
