package org.opentripplanner.profile;

import java.io.Serializable;
import java.util.Collection;

import org.python.google.common.primitives.Ints;

/** Represents a distribution of travel times to a destination. We use ints for speed; a true probability distribution should sum to Integer.MAX_VALUE */
public class Distribution implements Serializable {
    /** the number of minutes this distribution is offset from zero */
    public int offset; 
    
    /** is this a cumulative distribution? */
    public boolean cumulative;
    
    /** the values in the distribution. represented as ints for speed, from 0 (p = 0) to Distribution.MAX_VALUE (p = 1) */
    public double[] distribution;
    
    /**
     * Create an empty distribution of the given length.
     */
    public Distribution(int length) {
        offset = 0;
        distribution = new double[length];
    }
    
    /** integrate from from to to, inclusive */
    public double integrate (int from, int to) {
        double ret = 0;
        for (int i = from; i <= to; i++) {
            ret += get(i);
        }
        
        return ret;
    }
    
    /** sum the entire distribution */
    public double sum () {
        double ret = 0;
        for (double i : distribution) {
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
        ret.cumulative = true;
        
        double prev = 0;
        double accum = 0;
        
        for (int i = 0; i < this.distribution.length; i++) {            
            accum += this.distribution[i];
            ret.distribution[i] = accum;
            
            if (accum < prev - 1e15)
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
            double t = this.get(i);
            double o = other.get(i);
            ret.distribution[i - offset] = t + o - t * o;
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
            ret.distribution[i - offset] = get(i) * other.get(i);
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
            ret.distribution[i - offset] = get(i) + other.get(i); 
        }
        
        return ret;
    }
    
    
    public int size () {
        return offset + distribution.length;
    }
    
    public double get(int i) {
        if (i < offset)
            return 0;
        
        if (i >= offset + distribution.length) {
            if (!cumulative)
                return 0;
            else
                return distribution[distribution.length - 1];
        }            

        return distribution[i - offset];
    }
    
    /** get a value from the distribution ignoring the offset */
    private double getNoOffset(int i) {
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
                ret.distribution[i] += getNoOffset(j) * other.getNoOffset(i - j);
            }
        }
        
        return ret;
    }
    
    /** what is the expected value of this distribution? */
    public double expectation () {
        double accumulator = 0;
        double weights = 0;
        
        for (int i = 0; i < distribution.length; i++) {
            accumulator += distribution[i] * i;
            weights += distribution[i];
        }
        
        return (accumulator / weights) + offset;
    }
    
    /** convert this distribution to its complement, in place to save memory */
    public void complementInPlace() {
        for (int i = 0; i < distribution.length; i++) {
            distribution[i] = 1 - distribution[i];
        }
    }
    
    /** Create a uniform distribution of the given length */
    public static Distribution uniform(int length) {
        double probability = 1D / (double) length;
        
        Distribution ret = new Distribution(length);
        
        for (int i = 0; i < length; i++) {
            ret.distribution[i] = probability;
        }
        
        return ret;
    }
    
    /**
     * The minimum of independent distributions.
     * 
     * The probability of the minimum value being less than or equal to i is the complement
     * of the probability that no value is less than or equal to i, which we calculate as the
     * product of cumulative distributions.
     * 
     * See http://stats.stackexchange.com/questions/220/how-is-the-minimum-of-a-set-of-random-variables-distributed;
     * the argument there is for iid random variables, but minimal logic will extend the formula to independent
     * but not identically distributed variables.
     * 
     * Be aware that this function returns a <i>cumulative</i> distribution, but expects density distributions as input.
     */
    public static Distribution min (Collection<Distribution> distributions) {
        if (distributions.isEmpty())
            return null;
        
        if (distributions.size() == 1)
            return distributions.iterator().next().cumulative();
        
        Distribution[] cumulative = new Distribution[distributions.size()];
        
        int didx = 0;
        
        int maxLength = Integer.MIN_VALUE;
        int minOffset = Integer.MAX_VALUE;
        
        for (Distribution d : distributions) {
            cumulative[didx++] = d.cumulative();
            maxLength = Math.max(maxLength, d.size());
            minOffset = Math.min(minOffset, d.offset);
        }
        
        Distribution ret = new Distribution(maxLength - minOffset);
        ret.offset = minOffset;
        
        for (int i = minOffset; i < maxLength; i++) {
            ret.distribution[i - minOffset] = 1 - cumulative[0].get(i);
            
            for (int j = 1; j < cumulative.length; j++) {
                ret.distribution[i - minOffset] *= (1 - cumulative[j].get(i));
            }
        }
        
        ret.complementInPlace();
       
        ret.cumulative = true;
        
        return ret;
    }    
}
