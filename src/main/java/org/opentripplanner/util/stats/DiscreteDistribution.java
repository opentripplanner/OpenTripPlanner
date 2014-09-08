/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.util.stats;

import java.text.MessageFormat;

import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;

interface Quantifiable<K extends Quantifiable<K>> extends Comparable<K> {

    public double doubleValue();
}

/**
 * A discrete distribution on K (aka frequency).
 */
public class DiscreteDistribution<K extends Quantifiable<?>> {

    public static class NumberQuantifiable<K extends Number> implements
            Quantifiable<NumberQuantifiable<K>> {

        private K num;

        public NumberQuantifiable(K num) {
            this.num = num;
        }

        @Override
        public int hashCode() {
            return num.hashCode();
        }

        @Override
        public boolean equals(Object another) {
            if (another instanceof NumberQuantifiable) {
                @SuppressWarnings("unchecked")
                NumberQuantifiable<K> other = (NumberQuantifiable<K>) another;
                return other.num.equals(this.num);
            }
            return false;
        }

        @Override
        public int compareTo(NumberQuantifiable<K> o) {
            // This should be safe, even for integers
            return Double.compare(this.num.doubleValue(), o.num.doubleValue());
        }

        @Override
        public double doubleValue() {
            return num.doubleValue();
        }

        @Override
        public String toString() {
            return num.toString();
        }
    }

    public static class LogQuantifiable<K extends Number> implements
            Quantifiable<LogQuantifiable<K>> {

        private int log;

        private double mult;

        private K k;

        public LogQuantifiable(K k, double mult) {
            this.mult = mult;
            this.k = k;
            log = (int) Math.round(Math.log(k.doubleValue()) * mult);
        }

        @Override
        public int hashCode() {
            return log;
        }

        @Override
        public boolean equals(Object another) {
            if (another instanceof LogQuantifiable) {
                @SuppressWarnings("unchecked")
                LogQuantifiable<K> anotherLog = (LogQuantifiable<K>) another;
                return anotherLog.log == this.log;
            }
            return false;
        }

        @Override
        public int compareTo(LogQuantifiable<K> o) {
            // Do not compare on k here!
            return Integer.compare(log, o.log);
        }

        @Override
        public double doubleValue() {
            return k.doubleValue();
        }

        @Override
        public String toString() {
            double min = Math.exp(log / mult);
            double max = Math.exp((log + 1) / mult);
            return String.format("%.2f-%.2f", min, max);
        }
    }

    public static class ConstantQuantifiable<K extends Comparable<K>> implements
            Quantifiable<ConstantQuantifiable<K>> {

        private K k;

        public ConstantQuantifiable(K k) {
            this.k = k;
        }

        @Override
        public int hashCode() {
            return k.hashCode();
        }

        @Override
        public boolean equals(Object another) {
            if (another instanceof ConstantQuantifiable) {
                @SuppressWarnings("unchecked")
                ConstantQuantifiable<K> other = (ConstantQuantifiable<K>) another;
                return other.k.equals(this.k);
            }
            return false;
        }

        @Override
        public int compareTo(ConstantQuantifiable<K> o) {
            return k.compareTo(o.k);
        }

        @Override
        public double doubleValue() {
            return 1.0;
        }

        @Override
        public String toString() {
            return k.toString();
        }
    }

    private double totK = 0.0;

    private SortedMultiset<K> distribution = TreeMultiset.create();

    public void add(K k) {
        totK += k.doubleValue();
        distribution.add(k, 1);
    }

    public void add(K k, String sample) {
        totK += k.doubleValue();
        if (distribution.count(k) == 0) {
            System.out.println(k.doubleValue() + " => " + sample);
        }
        distribution.add(k, 1);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        int totCount = distribution.size();
        int minCount = Integer.MAX_VALUE;
        int maxCount = 0;
        for (Multiset.Entry<K> e : distribution.entrySet()) {
            int count = e.getCount();
            if (count < minCount)
                minCount = count;
            if (count > maxCount)
                maxCount = count;
        }
        sb.append(String.format("K: Total: %.02f, avg: %.02f, min: %s, max: %s\n", totK, totK * 1.0
                / totCount, distribution.firstEntry().getElement(), distribution.lastEntry()
                .getElement()));
        sb.append(String.format("C: Total: %d, min: %d, max: %d\n", totCount, minCount, maxCount));
        for (Multiset.Entry<K> e : distribution.entrySet()) {
            sb.append(MessageFormat.format("{0} : {1} {2}\n", e.getElement().toString(),
                    chart(e.getCount(), maxCount, 60), e.getCount()));
        }
        sb.append("----------------------------------------------------------------");
        return sb.toString();
    }

    private String chart(int x, int xMax, int len) {
        StringBuffer retval = new StringBuffer();
        for (int i = 0; i < Math.round(x * 1.0 * len / xMax); i++)
            retval.append("*");
        return retval.toString();
    }
}