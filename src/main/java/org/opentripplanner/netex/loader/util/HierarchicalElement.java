package org.opentripplanner.netex.loader.util;

/**
 * This is a hierarical wrapper around a singe element witch provide the
 * abillity to create <em>parent</em> and <em>child</em> relationship with
 * fallback from the child value to the parent value [if the child does not
 * have a value].
 * <p/>
 * There is no reference from the parent to the child, enableing garbage
 * collection of children, when not referenced by the outer context any more.
 *
 * @param <E> the element value type.
 */
public class HierarchicalElement<E> {
    private final HierarchicalElement<E> parent;

    private E element = null;

    /** Create a root for the hierarchy */
    public HierarchicalElement() {
        this(null);
    }

    /** Create a child of the given {@code parent}. */
    public HierarchicalElement(HierarchicalElement<E> parent) {
        this.parent = parent;
    }

    /**
     * Get element, if not found delegate up to the parent.
     * @return an {@code null} if no element is found.
     */
    public E get() {
        if(element != null) return element;
        if(parent == null) return null;
        else return parent.get();
    }

    /**
     * Set the {@code element} on the level in the hierarchy represented by {@code this} instance.
     */
    public void set(E element) {
        this.element = element;
    }
}
