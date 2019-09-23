/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
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

    public void setId(T id) {
        throw new UnsupportedOperationException(
                "It is not allowed to change the id for this type: " + getClass().getSimpleName()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TransitEntity<?> other = (TransitEntity<?>) obj;
        return getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
