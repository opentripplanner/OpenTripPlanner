/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util;

import java.util.Locale;

/**
 *
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
