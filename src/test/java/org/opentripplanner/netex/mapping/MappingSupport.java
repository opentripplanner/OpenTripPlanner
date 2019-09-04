package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.VersionOfObjectRefStructure;

import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * Test mapping support utility functions - shared between tests.
 */
class MappingSupport {
    /** private constructor to prevent instansiation of utility class */
    private MappingSupport() {
    }

    /**
     * Short for {@code createJaxbElement(createRef(id, clazz))}
     * @see #createRef(String, Class)
     * @see #createJaxbElement(Object)
     */
    static <T extends VersionOfObjectRefStructure> JAXBElement<T> createWrappedRef(String id, Class<T> clazz) {
        return createJaxbElement(createRef(id, clazz));
    }

    /**
     * Create a new instance of the given type T with the ref set to the given id.
     * @param id the ref is set to point to this id.
     * @param clazz the class used to create a new instance. The class must have a default constructor.
     * @param <T> the type of the created ref structure
     */
    @SuppressWarnings("unchecked")
    static <T extends VersionOfObjectRefStructure> T createRef(String id, Class<T> clazz) {
        try {
            return (T)clazz.newInstance().withRef(id);
        } catch (Exception e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Wrap an object in a JAXBElement instance.
     * @param value the value wrapped
     * @param <T> the value type
     * @return the value wrapped in a JAXBElement
     */
    static <T> JAXBElement<T> createJaxbElement(@NotNull T value) {
        return new JAXBElement<T>(new QName("x"), (Class<T>)value.getClass(), value);
    }
}
