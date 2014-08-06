/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.util;

import java.io.Serializable;
import java.util.Locale;

/**
 *
 * @author mabu
 */
public class NonLocalizedString implements I18NString, Serializable {
    private String name;

    public NonLocalizedString(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String toString(Locale locale) {
        return this.name;
    }

}
