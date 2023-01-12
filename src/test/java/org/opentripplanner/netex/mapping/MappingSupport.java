package org.opentripplanner.netex.mapping;

import jakarta.xml.bind.JAXBElement;
import javax.annotation.Nonnull;
import javax.xml.namespace.QName;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

/**
 * Test mapping support utility functions - shared between tests.
 */
public class MappingSupport {

  public static final FeedScopedIdFactory ID_FACTORY = new FeedScopedIdFactory("F");

  /** private constructor to prevent instansiation of utility class */
  private MappingSupport() {}

  /**
   * Short for {@code createJaxbElement(createRef(id, clazz))}
   *
   * @see #createRef(String, Class)
   * @see #createJaxbElement(Object)
   */
  public static <T extends VersionOfObjectRefStructure> JAXBElement<T> createWrappedRef(
    String id,
    Class<T> clazz
  ) {
    return createJaxbElement(createRef(id, clazz));
  }

  /**
   * Create a new instance of the given type T with the ref set to the given id.
   *
   * @param id    the ref is set to point to this id.
   * @param clazz the class used to create a new instance. The class must have a default
   *              constructor.
   * @param <T>   the type of the created ref structure
   */
  @SuppressWarnings("unchecked")
  public static <T extends VersionOfObjectRefStructure> T createRef(String id, Class<T> clazz) {
    try {
      return (T) clazz.newInstance().withRef(id);
    } catch (Exception e) {
      throw new RuntimeException(e.getLocalizedMessage(), e);
    }
  }

  /**
   * Wrap an object in a JAXBElement instance.
   *
   * @param value the value wrapped
   * @param <T>   the value type
   * @return the value wrapped in a JAXBElement
   */
  @SuppressWarnings("unchecked")
  public static <T> JAXBElement<T> createJaxbElement(@Nonnull T value) {
    return new JAXBElement<>(new QName("x"), (Class<T>) value.getClass(), value);
  }
}
