package org.opentripplanner.util;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * This is to support strings which can't be localized.
 *
 * It just returns string it is given in constructor.
 *
 * @author mabu
 */
public class NonLocalizedString implements I18NString, Serializable {
    private final String name;

    public NonLocalizedString(@Nonnull String name) {
        if (name == null) {
            throw new IllegalArgumentException();
        }
        this.name = name;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NonLocalizedString && this.name.equals(((NonLocalizedString)other).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
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
