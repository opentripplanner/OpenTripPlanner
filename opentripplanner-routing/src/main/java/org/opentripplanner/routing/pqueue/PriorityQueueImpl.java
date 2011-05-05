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

package org.opentripplanner.routing.pqueue;

import java.util.PriorityQueue;

public class PriorityQueueImpl<T> implements OTPPriorityQueue<T> {
    
    public static OTPPriorityQueueFactory FACTORY = new FactoryImpl();
    
    private PriorityQueue<Weighted<T>> _queue = new PriorityQueue<Weighted<T>>();

    @Override
    public void insert(T payload, double key) {
        _queue.add(new Weighted<T>(payload,key));
    }

    @Override
    public void insert_or_dec_key(T payload, double key) {
        insert(payload,key);
    }
    
    @Override
    public double peek_min_key() {
        Weighted<T> peek = _queue.peek();
        if( peek == null)
            return Double.NaN;
        return peek.weight;
    }
    
    @Override
    public T peek_min() {
        Weighted<T> peek = _queue.peek();
        if( peek == null)
            return null;
        return peek.payload;
    }

    @Override
    public T extract_min() {
        Weighted<T> min = _queue.poll();
        if( min == null)
            return null;
        return min.payload;
    }

    @Override
    public int size() {
        return _queue.size();
    }

    @Override
    public boolean empty() {
        return _queue.isEmpty();
    }

    private static final class Weighted<T> implements Comparable<Weighted<T>> {
        private final T payload;

        private final double weight;

        public Weighted(T payload, double weight) {
            this.payload = payload;
            this.weight = weight;
        }

        @Override
        public int compareTo(Weighted<T> o) {
            return Double.compare(this.weight, o.weight);
        }
    }
    
    private static class FactoryImpl implements OTPPriorityQueueFactory {
        @Override
        public <T> OTPPriorityQueue<T> create(int maxSize) {
            return new PriorityQueueImpl<T>();
        }
    }
}
