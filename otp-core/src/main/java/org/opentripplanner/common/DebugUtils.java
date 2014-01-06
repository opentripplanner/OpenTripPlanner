package org.opentripplanner.common;

public class DebugUtils {

    public static void mem() {
        System.gc();
        long memoryUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("memory used: " + memoryUsed);
    }

}
