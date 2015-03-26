package org.opentripplanner.profile;

import java.io.Serializable;

/** Represents a distribution of travel times to a destination. We use ints for speed; a true probability distribution should sum to Integer.MAX_VALUE */
public class Distribution implements Serializable {
    /** the number of minutes this distribution is offset from zero */
    public int offset; 
    
    /** the values in the distribution. represented as ints for speed, from 0 (p = 0) to Integer.MAX_VALUE (p = 1) */
    public int[] distribution;
    
    /**
     * Create an empty distribution of the given length.
     */
    public Distribution(int length) {
        offset = 0;
        distribution = new int[length];
    }
    
    /** integrate from from to to, inclusive */
    public int integrate (int from, int to) {
        int ret = 0;
        for (int i = from; i <= to; i++) {
            ret += get(i);
        }
        
        return ret;
    }
    
    public int get(int i) {
        if (i < offset || i - offset >= distribution.length)
            return 0;

        return distribution[i - offset];
    }
    
    /** get a value from the distribution ignoring the offset */
    private int getNoOffset(int i) {
        if (i >= distribution.length)
            return 0;
        
        return distribution[i];
    }
    
    /** convolve this distribution with another */
    public Distribution convolve (Distribution other) {
        Distribution ret = new Distribution(this.distribution.length + other.distribution.length);
        ret.offset = this.offset + other.offset;
        
        for (int i = 0; i < ret.distribution.length - 1; i++) {
            // loop over all the ways they could sum to i
            for (int j = 0; j <= i; j++) {
                // note: there is an issue with sampling here, and discrete/continuous representations
                // this probability represents the probability that this vehicle arrives in between
                // j and j + 1 half-seconds (or whatever sampling rate you choose to use), and the other
                // vehicle arrives in between i - j and i - j + 1 half-seconds. That's not the same as
                // saying that their sum is between i and i + 1, because the width of both is one, so the
                // width together is two.
                
                // The answer is probably to band-limit the uniform distribution
                int probability = multiply(getNoOffset(j), other.getNoOffset(i - j));
                ret.distribution[i] += probability;
            }
        }
        
        return ret;
    }
    
    /** Create a uniform distribution of the given length */
    public static Distribution uniform(int length) {
        int probability = Integer.MAX_VALUE / length;
        
        Distribution ret = new Distribution(length);
        
        for (int i = 0; i < length; i++) {
            ret.distribution[i] = probability;
        }
        
        return ret;
    }
    
    /** "multiply" two probabilities expressed as integers */
    public static int multiply (int a, int b) {
        // TODO: efficient way to do this?
        return (int) (((long) a) * ((long) b) / ((long) Integer.MAX_VALUE));
    }
}
