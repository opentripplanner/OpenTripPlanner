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
public class LocalizedString implements I18NString {
    
    private String key;
    private String[] params;

    public LocalizedString(String key, String[] params) {
        this.key = key;
        this.params = params;
    }

    @Override
    public String toString() {
        return this.toString(Locale.getDefault());
    }    

    @Override
    public String toString(Locale locale) {
        //TODO: problem is that translations has {name} when 0,1,2,n is expected
        return String.format(ResourceBundleSingleton.INSTANCE.localize(this.key, locale), (Object[]) params);
    }

}
