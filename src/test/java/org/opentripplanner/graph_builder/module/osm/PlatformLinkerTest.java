package org.opentripplanner.graph_builder.module.osm;


import org.junit.Test;

public class PlatformLinkerTest {

    @Test
    public void testRayCasting(){

        final double[][] platform = {{10.5242855, 59.8925348}, {10.524743, 59.8927012}, { 10.5248089, 59.8926397}, {10.5243485, 59.8924888}};

        double[] testPointInside = { 10.524729, 59.8926474};
        double[] testPointOutside = { 10.5249007, 59.8925344};

        assert(PlatformLinker.contains(platform, testPointInside));
        assert(!PlatformLinker.contains(platform, testPointOutside));
    }

    @Test
    public void testRayCasting_Hellerud(){
        final double[][] platform = {{ 10.830791300000001, 59.910225700000005},
                { 10.830805, 59.910188700000006},
                { 10.8288492, 59.910006100000004},
                { 10.828837700000001, 59.910037},
                { 10.8297421, 59.9101177},
                { 10.8297992, 59.91012980000001},
                { 10.8300367, 59.910152700000005},
                { 10.8300263, 59.910181},
                { 10.830367, 59.910212800000004},
                { 10.8303726, 59.910197600000004},
                { 10.830376600000001, 59.91018690000001}
        };

        double[] testPointOutside = { 10.6923612, 59.910212800000005};
        assert(!PlatformLinker.contains(platform, testPointOutside));
    }

}
