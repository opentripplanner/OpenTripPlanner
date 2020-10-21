package org.opentripplanner.model;

import java.io.Serializable;

/**
 * All OTP Transit entities should extend this class. The purpose of the class is to enforce
 * a common implementation of the identity:
 * <ol>
 *   <li>
 *     {@code id} - All entities should have an id. The id should be unique within the
 *     context the entity live. For aggregates witch live in a global space the id must be unique.
 *     This apply to all entities listed in the index service.
 *   </li>
 *   <li>
 *     The {@code hashCode()/equals()} method is consistent and based on the id (identity). We
 *     frequently use this to index and lookup entities.
 *   </li>
 * </ol>
 */
public abstract class TransitEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private final FeedScopedId id;

    public TransitEntity(FeedScopedId id) {
        this.id = id;
    }

    public final FeedScopedId getId() {
        return id;
    }

    /**
     * Uses the  {@code id} for identity. We could use the {@link Object#equals(Object)} method,
     * but this causes the equals to fail in cases were the same entity is created twice - for
     * example after reloading a serialized instance.
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TransitEntity other = (TransitEntity) obj;
        return getId().equals(other.getId());
    }

    @Override
    final public int hashCode() {
        return getId().hashCode();
    }

    /**
     * Provide a default toString implementation with class name and id.
     */
    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + " " + getId() + ">";
    }
}
