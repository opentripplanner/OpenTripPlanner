package org.opentripplanner.graph_builder.module.osm;


import org.junit.Test;

public class PlatformLinkerTest {

    @Test
    public void testRayCasting(){

        final double[][] platform = {{ 59.8925348, 10.5242855}, { 59.8927012, 10.524743}, { 59.8926397, 10.5248089}, {59.8924888, 10.5243485}};

        double[] testPointInside = { 59.8926474, 10.524729};
        double[] testPointOutside = { 59.8925344, 10.5249007};

        assert(PlatformLinker.contains(platform, testPointInside));
        assert(!PlatformLinker.contains(platform, testPointOutside));
    }
}
