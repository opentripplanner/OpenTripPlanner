package org.opentripplanner.routing.util;

public class SlopeCosts {
    public final boolean flattened;
    public final double slopeSpeedFactor; // The slope speed factor multiplier, w/o units
    public final double slopeWorkFactor; // The slope work factor in joules per meters at 5 m/s
    public final double maxSlope; // Max{abs(slope)}
    public final double slopeSafetyCost; // An additional safety cost caused by the slope
    public final double lengthMultiplier; // Multiplier to get true length based on flat (projected) length
    public final byte[] gradients; // array of gradients as percents
    public final short[] gradientLengths; // array of the length of each gradient in meters
    public final float[] gradientCdas; // array of the resistive drag force component at the corresonding gradient

    public SlopeCosts(double slopeSpeedFactor, double slopeWorkFactor, double slopeSafetyCost,
                      double maxSlope, double lengthMultiplier, boolean flattened, byte[] gradients,
                      short[] gradientLengths, float[] gradientCdas) {
        this.slopeSpeedFactor = slopeSpeedFactor;
        this.slopeWorkFactor = slopeWorkFactor;
        this.slopeSafetyCost = slopeSafetyCost;
        this.maxSlope = maxSlope;
        this.lengthMultiplier = lengthMultiplier;
        this.flattened = flattened;
        this.gradients = gradients;
        this.gradientLengths = gradientLengths;
        this.gradientCdas = gradientCdas;
    }
}
