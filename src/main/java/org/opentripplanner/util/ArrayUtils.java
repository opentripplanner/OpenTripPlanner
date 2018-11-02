package org.opentripplanner.util;


/**
 * This class replaces a small part of the functionality provided by ArrayUtils from Apache Commons.
 */
public class ArrayUtils {
    public static boolean contains(Object array[], Object object) {
        if (array != null) {
            if (object != null) {
                for (Object element : array) if (object.equals(element)) return true;
            } else {
                for (Object element : array) if (element == null) return true;
            }
        }
        return false;
    }
}
