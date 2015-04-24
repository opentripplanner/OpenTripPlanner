package org.opentripplanner.profile;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

import java.io.Serializable;

/**
 * Created by abyrd on 23/04/15.
 */
public class Jagged2DIntArray implements Serializable {

    public final TIntArrayList offsets;
    public final TIntArrayList data;

    public Jagged2DIntArray () {
        this(10, 10);
    }

    public Jagged2DIntArray (int expectedRows, int  averageColumnSize) {
        offsets = new TIntArrayList(expectedRows);
        data = new TIntArrayList(expectedRows * averageColumnSize);
    }

    public void appendRow (TIntCollection ints) {
        offsets.add(data.size());
        data.addAll(ints);
    }

    public void appendRow (int[] ints) {
        offsets.add(data.size());
        data.addAll(ints);
    }

    public void finish () {
        offsets.add(data.size());
    }

    public TIntIterator rowIterator (int row) {
        return new JaggedIterator(this, row);
    }

    public class JaggedIterator implements TIntIterator {

        int beginIndexInclusive;
        int endIndexExclusive;
        int position;

        public JaggedIterator (Jagged2DIntArray over, int rowIndex) {
            beginIndexInclusive = over.offsets.get(rowIndex);
            endIndexExclusive = over.offsets.get(rowIndex + 1);
            position = beginIndexInclusive;
        }

        @Override
        public int next() {
            return data.get(position++);
        }

        @Override
        public boolean hasNext() {
            return position < endIndexExclusive;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
