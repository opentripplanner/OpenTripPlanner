package org.opentripplanner.netex.support;

import jakarta.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Utility class for processing JAXB objects.
 */
public final class JAXBUtils {

  private JAXBUtils() {}

  /**
   * Transform a collection of JAXBElement wrappers into a stream containing the unwrapped values.
   * Null values are filtered out. Note:
   * <ul>A null JAXBElement represents an element not present in the document.</ul>
   * <ul>A non-null JAXBElement wrapping a null value represents an xsi:nil element.</ul>
   * There is currently no nillable elements in NeTEx.
   *
   * @param type the Java type of the wrapped element.
   * @param c    the collection of JAXBElement wrappers.
   */
  public static <S extends JAXBElement<?>, T> Stream<T> streamJAXBElementValue(
    Class<T> type,
    Collection<S> c
  ) {
    return c
      .stream()
      .filter(Objects::nonNull)
      .map(JAXBElement::getValue)
      .filter(Objects::nonNull)
      .filter(type::isInstance)
      .map(type::cast);
  }

  /**
   * Transform a collection of JAXBElement wrappers into a stream containing the unwrapped values
   * and apply a consumer on each value. Null values are filtered out.
   *
   * @param type the Java type of the wrapped element.
   * @param c    the collection of JAXBElement wrappers.
   * @param body a consumer to apply on each unwrapped value.
   */
  public static <S extends JAXBElement<?>, T> void forEachJAXBElementValue(
    Class<T> type,
    Collection<S> c,
    Consumer<T> body
  ) {
    streamJAXBElementValue(type, c).forEach(body);
  }
}
