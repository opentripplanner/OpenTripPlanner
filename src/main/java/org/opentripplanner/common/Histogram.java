package org.opentripplanner.common;

import com.google.common.base.Strings;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.stream.IntStream;

/**
 * For design and debugging purposes, a simple class that tracks the frequency of different numbers.
 */
public class Histogram {

    private String title;
    private TIntIntMap bins = new TIntIntHashMap();
    private int maxBin = Integer.MIN_VALUE;
    private int minBin = Integer.MAX_VALUE;
    private long count = 0;
    private int maxVal;

    public Histogram (String title) {
        this.title = title;
    }

    public void add (int i) {
        count++;
        int binVal = bins.adjustOrPutValue(i, 1, 1);

        if (binVal > maxVal)
            maxVal = binVal;

        if (i > maxBin) {
            maxBin = i;
        }

        if (i < minBin) {
            minBin = i;
        }
    }

    private static String makeBar (int value, int max) {
        final int WIDTH = 20;
        int n = value * WIDTH / max;
        String bar = Strings.repeat("#", n);
        String space = Strings.repeat("_", WIDTH - n);
        return bar + space + "  ";
    }

    public void display () {
        int[] lessEqual = new int[maxBin + 1];
        System.out.println("--- Histogram: " + title + " ---");
        System.out.println(" n       ==      <=       >");
        int sum = 0;
        int maxCount = 0;
        for (int i = minBin; i <= maxBin; i++) {
            int n = bins.get(i);
            if (n > maxCount) {
                maxCount = n;
            }
            sum += n;
            lessEqual[i] = sum;
        }
        // Sum now equals the sum of all bins.
        for (int i = 0; i <= maxBin; i++) {
            if (((double)lessEqual[i]) / sum > 0.999) {
                System.out.println("Ending display at 99.9% of total objects.");
                break;
            }
            System.out.printf("%2d: %7d %7d %7d ", i, bins.get(i), lessEqual[i], sum - lessEqual[i]);
            System.out.print(makeBar(bins.get(i), maxCount));
            System.out.print(makeBar(lessEqual[i], sum));
            System.out.print(makeBar(sum - lessEqual[i], sum));
            System.out.println();
        }
        System.out.println();
    }

    public void displayHorizontal () {
        System.out.println("--- Histogram: " + title + " ---");

        // TODO: horizontal scale
        double vscale = 30d / maxVal;

        for (int i = 0; i < 30; i++) {
            StringBuilder row = new StringBuilder(maxBin - minBin + 1);

            int minValToDisplayThisRow = (int) ((30 - i) / vscale);
            for (int j = minBin; j <= maxBin; j++) {
                if (bins.get(j) > minValToDisplayThisRow)
                    row.append('#');
                else
                    row.append(' ');
            }

            System.out.println(row);
        }

        // put a mark at zero and at the ends
        if (minBin < 0 && maxBin > 0) {
            StringBuilder ticks = new StringBuilder();
            for (int i = minBin; i < 0; i++)
                ticks.append(' ');
            ticks.append('|');
            System.out.println(ticks);
        }

        StringBuilder row = new StringBuilder();
        for (int i = minBin; i < maxBin; i++) {
            row.append(' ');
        }

        String start = new Integer(minBin).toString();
        row.replace(0, start.length(), start);
        String end = new Integer(maxBin).toString();
        row.replace(row.length() - end.length(), row.length(), end);
        System.out.println(row);
    }

    public int mean() {
        long sum = 0;
        for (TIntIntIterator it = bins.iterator(); it.hasNext();) {
            it.advance();

            sum += it.key() * it.value();
        }

        return (int) (sum / count);
    }

    public static void main (String... args) {
        System.out.println("Testing histogram store with normal distribution, mean 0");
        Histogram h = new Histogram("Normal");

        MersenneTwister mt = new MersenneTwister();

        IntStream.range(0, 1000000).map(i -> (int) Math.round(mt.nextGaussian() * 20 + 2.5)).forEach(h::add);

        h.displayHorizontal();
        System.out.println("mean: " + h.mean());
    }
}
