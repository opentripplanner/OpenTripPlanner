package org.opentripplanner.framework.doc;

/**
 * This interface is used to allow the enum documentation to be written in one place, and reused in
 * config and APIs.
 * <p>
 * Markdown should be used for formatting the doc.
 */
public interface DocumentedEnum<E extends Enum<E>> {
  /**
   * Write a general description of the enum type. Avoid including default value, or
   * including enum values in the description since this is use-case specific.
   */
  String typeDescription();

  /**
   * Return a description of the enum value.
   */
  String enumValueDescription();

  /**
   * Cast an instance of this interface to a generic enum type. This is valid, because
   * this interface should only be implemented by enum types.
   */
  @SuppressWarnings("unchecked")
  default E castToEnum() {
    return (E) this;
  }
}
