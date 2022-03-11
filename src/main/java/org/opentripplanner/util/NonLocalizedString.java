package org.opentripplanner.util;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is to support strings which can't be localized.
 *
 * It just returns string it is given in constructor.
 *
 * @author mabu
 */
public class NonLocalizedString implements I18NString, Serializable {
    private final String name;

    public NonLocalizedString(String name) {
        this.name = name;
    }

    /**
     * Check if name is non-null and returns an instance of {@link NonLocalizedString}, otherwise
     * returns null.
     */
    @Nullable
    public static NonLocalizedString ofNullable(@Nullable String name){
        if(name == null) { return null; }
        return new NonLocalizedString(name);
    }

    /**
     * Check if name is non-null and returns an instance of {@link NonLocalizedString}, otherwise
     * returns a {@link NonLocalizedString} with the default name.
     */
    @Nonnull
    public static NonLocalizedString ofNullableOrElse(
            @Nullable String name,
            @Nonnull String defaultName
    ){
        return new NonLocalizedString(name == null ? defaultName : name);
    }

    /**
     * Check if name is non-null and returns an instance of {@link NonLocalizedString},  otherwise
     *      * returns a {@link I18NString} with the default name.
     */
    @Nonnull
    public static I18NString ofNullableOrElse(
            @Nullable String name,
            @Nonnull I18NString defaultName
    ){
        return name == null ? defaultName : new NonLocalizedString(name);
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
