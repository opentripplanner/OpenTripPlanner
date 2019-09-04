package org.opentripplanner.model.impl;

import org.opentripplanner.model.IdentityBean;

import java.util.List;

/**
 * Utility class to help inserting none existing ids.
 * @see #generateNoneExistentIds(List)
 */
class GenerateMissingIds {

    /** private to prevent instantiation of utility class */
    private GenerateMissingIds() {}

    /**
     * Iterate over the entities; First to find the larged numerical id, then
     * set all ids that is {@code 0} or {@code null} to a uniq nuberical id.
     */
    static <T extends IdentityBean<String>> void generateNoneExistentIds(List<T> entities) {
        int maxId = 0;

        for (T it : entities) {
            maxId = Math.max(maxId, parseId(it));
        }

        for (T it : entities) {
            if(parseId(it) == 0) {
                it.setId(Integer.toString(++maxId));
            }
        }
    }

    /** pares entity id as int, if not int -1 is resturned, if 0 or null zero is returned. */
    private static int parseId(IdentityBean<String> entity) {
        try {
            String id = entity.getId();
            return id == null ? 0 : Integer.parseInt(id);
        }
        catch (NumberFormatException ignore) {
            return -1;
        }
    }
}
