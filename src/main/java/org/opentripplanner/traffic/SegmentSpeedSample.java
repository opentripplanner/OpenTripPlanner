package org.opentripplanner.traffic;

import io.opentraffic.engine.data.pbf.ExchangeFormat;
import io.opentraffic.engine.data.stats.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Represents speeds at particular times of day.
 */
public class SegmentSpeedSample implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(SegmentSpeedSample.class);

    private static final double KMH_TO_MS = 1000d / 3600d;

    /**
     * the overall average speed on this segment, in centimeters per second, with -32,768 representing 0.
     * This allows representation of speeds up to 2359 kilometers per hour.
     */
    private final short average;

    /**
     * The average speeds by hour of week, with 0 being midnight Monday morning GMT.
     * Coded as above.
     */
    private final short[] hourBins;

    /** Get a speed estimate in meters per second for the time specified (in milliseconds since the epoch) */
    public double getSpeed (long time) {
        if (hourBins == null)
            return decodeSpeed(average);

        // figure out the hour bin
        Instant instant = Instant.ofEpochMilli(time);

        OffsetDateTime dt = instant.atOffset(ZoneOffset.UTC);

        // 0 (Monday) to 6 (Sunday) after subtraction
        int day = DayOfWeek.from(dt).getValue() - 1;
        int hour = dt.getHour();

        int hourBin = day * 24 + hour;
        return decodeSpeed(hourBins[hourBin]);
    }

    /** Decode a speed to meters per second from its short representation */
    private double decodeSpeed (short speed) {
        return (((double) speed) - Short.MIN_VALUE) / 100d;
    }

    /** Encode a speed stored as meters per second to its short representation. */
    private short encodeSpeed (double speed) {
        if (speed < 0)
            throw new UnsupportedOperationException("negative speeds do not exist.");

        if (speed > 65535 / 100d) {
            LOG.warn("Speed is greater than 2359.26 kilometers per hour, clamping. However, are you certain that there is a road with a speed this fast?");
            return Short.MAX_VALUE;
        }


        return (short) (speed * 100 - Short.MIN_VALUE);
    }

    /** Create a speed sample from an OpenTraffic PBF stats object */
    public SegmentSpeedSample(ExchangeFormat.BaselineStats stats) {
        float avg = stats.getAverageSpeed();

        if (Float.isNaN(avg)) {
            LOG.error("Invalid speed sample: average speed is NaN");
            throw new IllegalArgumentException("Overall average speed for a sample is NaN.");
        }

        this.average = encodeSpeed(avg * KMH_TO_MS);

        int count = stats.getHourOfWeekAveragesCount();

        if (count == 7 * 24) {
            hourBins = new short[count];

            for (int i = 0; i < count; i++) {
                float speed = stats.getHourOfWeekAverages(i);

                if (!Float.isNaN(speed))
                    hourBins[i] = encodeSpeed(speed * KMH_TO_MS);
                else
                    hourBins[i] = average;
            }
        }
        else {
            if (count > 0 )
                LOG.error("Expected {} hours in speed sample, found {}", 7 * 24, count);

            hourBins = null;
        }
    }

    /** Create a speed sample from an OpenTraffic stats object directly */
    public SegmentSpeedSample(SummaryStatistics stats) {
        double avg = stats.getMean();

        if (Double.isNaN(avg)) {
            LOG.error("Invalid speed sample: average speed is NaN");
            throw new IllegalArgumentException("Overall average speed for a sample is NaN.");
        }

        this.average = encodeSpeed(avg);

        hourBins = new short[7 * 24];

        for (int i = 0; i < 7 * 24; i++) {
            double speed = stats.getMean(); //TODO make it possible to grab summary by hour

            if (!Double.isNaN(speed))
                hourBins[i] = encodeSpeed(speed);
            else
                hourBins[i] = average;
        }

    }

    /** create a speed sample using a function */
    public SegmentSpeedSample(double averageSpeed, double[] hourBins) {
        this.average = encodeSpeed(averageSpeed);
        this.hourBins = new short[hourBins.length];

        for (int i = 0; i < hourBins.length; i++) {
            this.hourBins[i] = Double.isNaN(hourBins[i]) ? this.average : encodeSpeed(hourBins[i]);
        }
    }
}
