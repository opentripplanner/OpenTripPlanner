package org.opentripplanner.profile;

import java.io.Serializable;
import java.util.Arrays;

public class Contiguous2DShortArray implements Serializable {

    final int dx, dy;
    final short[] values;

    public Contiguous2DShortArray(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
        values = new short[dx * dy];
    }

    public void initialize (short initialValue) {
        Arrays.fill(values, initialValue);
    }

    public short get (int x, int y) {
        return values[x * dy + y];
    }

    public void set (int x, int y, short value) {
        values[x * dy + y] = value;
    }

    public boolean setIfLess (int x, int y, short value) {
        int index = x * dy + y;
        if (values[index] > value) {
            values[index] = value;
            return true;
        }
        return false;
    }

    public void adjust (int x, int y, short amount) {
        int index = x * dy + y;
        values[index] += amount;
    }

}
