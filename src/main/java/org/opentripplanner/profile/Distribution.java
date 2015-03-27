package org.opentripplanner.profile;

import java.io.Serializable;

import org.python.google.common.primitives.Ints;

/** Represents a distribution of travel times to a destination. We use ints for speed; a true probability distribution should sum to Integer.MAX_VALUE */
public class Distribution implements Serializable {
    /** the number of minutes this distribution is offset from zero */
    public int offset; 
    
    /** The maximum value (p = 1). Slightly smaller than Integer.MAX_VALUE to preven overflow when computing complement distributions */
    public static final int MAX_VALUE = Integer.MAX_VALUE - 100000;
    
    /** the values in the distribution. represented as ints for speed, from 0 (p = 0) to Distribution.MAX_VALUE (p = 1) */
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
    
    /** sum the entire distribution */
    public int sum () {
        int ret = 0;
        for (int i : distribution) {
            ret += i;
        }
        
        return ret;
    }
    
    /**
     * Create a cumulative distribution from this distribution.
     */
    public Distribution cumulative () {
        Distribution ret = new Distribution(this.distribution.length);
        ret.offset = this.offset;
        
        int prev = 0;
        int accum = 0;
        
        for (int i = 0; i < this.distribution.length; i++) {            
            accum += this.distribution[i];
            ret.distribution[i] = accum;
            
            if (accum < prev)
                throw new IllegalStateException("Overflow when creating cumulative distribution!");
            
            prev = accum;
        }
        
        return ret;
    }
    
    /**
     * This distribution or the other one, for independent events.
     * P(A or B) = P(A) + P(B) - P(A*B)
     */
    public Distribution or (Distribution other) {
        int offset = Math.min(this.offset, other.offset);
        int length = Math.max(this.size(), other.size()) - offset; 
        Distribution ret = new Distribution(length);
        ret.offset = offset;
        
        for (int i = offset; i < ret.size(); i++) {
            // note that order of operations is important to prevent overflow.
            ret.distribution[i - offset] = -multiply(get(i), other.get(i)) + get(i) + other.get(i); 
        }
        
        return ret;
    }
    
    /**
     * This distribution and the other one, for independent events.
     */
    public Distribution and (Distribution other) {
        // we can safely ignore anything where either distribution is zero
        int offset = Math.max(this.offset, other.offset);
        int length = Math.min(this.size(), other.size()) - offset;
        
        // non-overlapping distributions
        if (length < 0)
            return new Distribution(0);
        
        Distribution ret = new Distribution(length);
        ret.offset = offset;
        
        for (int i = offset; i < ret.size(); i++) {
            ret.distribution[i - offset] = multiply(get(i), other.get(i));
        }
        
        return ret;
    }
    
    /** the sum of two distributions, i.e. stacked atop each other. Also known as or for mutually exclusive events. */
    public Distribution orMutuallyExclusive (Distribution other) {
        int offset = Math.min(this.offset, other.offset);
        int length = Math.max(this.size(), other.size()) - offset; 
        Distribution ret = new Distribution(length);
        ret.offset = offset;
        
        for (int i = offset; i < ret.size(); i++) {
            // Danger, Will Robinson, danger: overflow potential if not used correctly
            ret.distribution[i - offset] = get(i) + other.get(i); 
        }
        
        return ret;
    }
    
    public boolean valid () {
        return distribution.length == 0 || Ints.min(distribution) >= 0;
    }
    
    public int size () {
        return offset + distribution.length;
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
                
                // The answer is probably to band-limit the uniform distribution.
                int probability = multiply(getNoOffset(j), other.getNoOffset(i - j));
                ret.distribution[i] += probability;
            }
        }
        
        return ret;
    }
    
    /** what is the expected value of this distribution? */
    public int expectation () {
        long accumulator = 0;
        long weights = 0;
        
        for (int i = 0; i < distribution.length; i++) {
            accumulator += ((long) distribution[i]) * ((long) i);
            weights += distribution[i];
        }
        
        return ((int) (accumulator / weights)) + offset;
    }
    
    /** convert this distribution to its complement, in place to save memory */
    public void complementInPlace() {
        for (int i = 0; i < distribution.length; i++) {
            distribution[i] = MAX_VALUE - distribution[i];
            
            if (distribution[i] < 0)
                throw new IllegalStateException("Overflow when computing complement!");
        }
    }
    
    /** Create a uniform distribution of the given length */
    public static Distribution uniform(int length) {
        int probability = MAX_VALUE / length;
        
        Distribution ret = new Distribution(length);
        
        for (int i = 0; i < length; i++) {
            ret.distribution[i] = probability;
        }
        
        return ret;
    }
    
    /** "multiply" two probabilities expressed as integers */
    public static int multiply (int a, int b) {
        // TODO: efficient way to do this?
        return (int) (((long) a) * ((long) b) / ((long) MAX_VALUE));
    }
}
