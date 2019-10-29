/*
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentripplanner.transit.raptor.speed_test.api.model;

import java.io.Serializable;

public class AgencyAndId implements Serializable, Comparable<org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId> {

    private static final char ID_SEPARATOR = '_';

    private static final long serialVersionUID = 1L;

    private final String agencyId;

    private final String id;

    private transient int hash = 0;

    public AgencyAndId(String agencyId, String id) {
        this.agencyId = agencyId;
        this.id = id;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public String getId() {
        return id;
    }

    public boolean hasValues() {
        return this.agencyId != null && this.id != null;
    }

    public int compareTo(org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId o) {
        int c = this.agencyId.compareTo(o.agencyId);
        if (c == 0)
            c = this.id.compareTo(o.id);
        return c;
    }

    /**
     * Given an id of the form "agencyId_entityId", parses into a
     * {@link org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId} id object.
     *
     * @param value id of the form "agencyId_entityId"
     * @return an id object
     */
    public static org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId convertFromString(String value, char separator) {
        int index = value.indexOf(separator);
        if (index == -1) {
            throw new IllegalStateException("invalid agency-and-id: " + value);
        } else {
            return new org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId(value.substring(0, index), value.substring(index + 1));
        }
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = agencyId.hashCode() ^ id.hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId))
            return false;
        org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId other = (org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId) obj;
        if (!id.equals(other.id))
            return false;
        if (!agencyId.equals(other.agencyId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return convertToString(this);
    }

    /**
     * Given an id of the form "agencyId_entityId", parses into a
     * {@link org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId} id object.
     *
     * @param value id of the form "agencyId_entityId"
     * @return an id object
     * @throws IllegalArgumentException if the id cannot be parsed
     */
    public static org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId convertFromString(String value) throws IllegalArgumentException {
        if (value == null || value.isEmpty())
            return null;
        int index = value.indexOf(ID_SEPARATOR);
        if (index == -1) {
            throw new IllegalArgumentException("invalid agency-and-id: " + value);
        } else {
            return new org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId(value.substring(0, index), value.substring(index + 1));
        }
    }

    /**
     * Given an {@link org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId} object, creates a string representation of the
     * form "agencyId_entityId"
     *
     * @param aid an id object
     * @return a string representation of the form "agencyId_entityId"
     */
    public static String convertToString(org.opentripplanner.transit.raptor.speed_test.api.model.AgencyAndId aid) {
        if (aid == null)
            return null;
        return concatenateId(aid.getAgencyId(), aid.getId());
    }

    /**
     * Concatenate agencyId and id into a string.
     */
    public static String concatenateId(String agencyId, String id) {
        return agencyId + ID_SEPARATOR + id;
    }

}
