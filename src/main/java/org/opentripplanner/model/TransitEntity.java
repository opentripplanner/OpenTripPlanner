package org.opentripplanner.model;

import java.io.Serializable;

/**
 * All OTP Transit entities should extend this class.
 *
 * @param <T> The ID type - Some Entities uses String and others uses {@link FeedScopedId} so this
 *           need to be generic.
 */
public abstract class TransitEntity<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    public abstract T getId();

    /**
     * Override this method to allow the id to be set. The default implementation throw a
     * {@link UnsupportedOperationException}.
     */
    public void setId(T id) {
        throw new UnsupportedOperationException(
                "It is not allowed to change the id for this type: " + getClass().getSimpleName()
        );
    }

    /**
     * Uses the  {@code id} for identity. We could use the {@link Object#equals(Object)} method,
     * but this causes the equals to fail in cases were the same entity is created twice - for
     * example after reloading a serialized instance.
     */
    @Override
    final public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TransitEntity<?> other = (TransitEntity<?>) obj;
        return getId().equals(other.getId());
    }

    @Override
    final public int hashCode() {
        return getId().hashCode();
    }
}
