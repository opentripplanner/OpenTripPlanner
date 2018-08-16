/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import java.io.Serializable;

public abstract class IdentityBean<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    public abstract T getId();

    public abstract void setId(T id);

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof IdentityBean<?>) || getClass() != obj.getClass()) {
            return false;
        }
        IdentityBean<?> entity = (IdentityBean<?>) obj;
        return getId().equals(entity.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
