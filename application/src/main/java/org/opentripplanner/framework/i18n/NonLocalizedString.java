package org.opentripplanner.framework.i18n;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * This is to support strings which can't be localized.
 * <p>
 * It just returns string it is given in constructor.
 *
 * @author mabu
 */
public class NonLocalizedString implements I18NString, Serializable {

  private final String name;

  public NonLocalizedString(String name) {
    this.name = Objects.requireNonNull(name);
  }

  /**
   * Check if name is non-null and returns an instance of {@link NonLocalizedString}, otherwise
   * returns null.
   */
  @Nullable
  public static NonLocalizedString ofNullable(@Nullable String name) {
    if (name == null) {
      return null;
    }
    return new NonLocalizedString(name);
  }

  /**
   * Create a new instance from the given wrapper type, if the input is not {@code null}, else
   * return {@code null}.
   */
  public static <W> NonLocalizedString ofNullable(
    @Nullable W wrapper,
    Function<W, String> getValueOp
  ) {
    return wrapper == null ? null : new NonLocalizedString(getValueOp.apply(wrapper));
  }

  /**
   * Create a new instance from the given wrapper type, if the input is not {@code null}, else
   * return {@code null}.
   */
  public static <W> NonLocalizedString ofNullable(
    @Nullable W wrapper,
    Function<W, String> getValueOp,
    String defaultValue
  ) {
    return new NonLocalizedString(wrapper == null ? defaultValue : getValueOp.apply(wrapper));
  }

  /**
   * Check if name is non-null and returns an instance of {@link NonLocalizedString}, otherwise
   * returns a {@link NonLocalizedString} with the default name.
   */
  public static NonLocalizedString ofNullableOrElse(@Nullable String name, String defaultName) {
    return new NonLocalizedString(name == null ? defaultName : name);
  }

  /**
   * Check if name is non-null and returns an instance of {@link NonLocalizedString},  otherwise
   * returns a {@link I18NString} with the default name.
   */
  public static I18NString ofNullableOrElse(@Nullable String name, I18NString defaultName) {
    return name == null ? defaultName : new NonLocalizedString(name);
  }

  public static I18NString ofNumber(Number startPriceDurationHours) {
    return new NonLocalizedString(startPriceDurationHours.toString());
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof NonLocalizedString that && this.name.equals(that.name));
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
