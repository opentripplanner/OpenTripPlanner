package org.opentripplanner.streets;

import com.google.common.base.Strings;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * For design and debugging purposes, a simple class that tracks the frequency of different numbers.
 */
public class Histogram {

    private String title;
    private TIntIntMap bins = new TIntIntHashMap();
    private int maxBin = 0;

    public Histogram (String title) {
        this.title = title;
    }

    public void add (int i) {
        bins.adjustOrPutValue(i, 1, 1);
        if (i > maxBin) {
            maxBin = i;
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
        for (int i = 0; i <= maxBin; i++) {
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

}
