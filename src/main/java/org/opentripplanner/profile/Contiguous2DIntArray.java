package org.opentripplanner.profile;


import java.io.Serializable;
import java.util.Arrays;

public class Contiguous2DIntArray implements Serializable {

    final int dx, dy;
    final int[] values;

    public Contiguous2DIntArray (int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
        values = new int[dx * dy];
    }

    public void initialize (int initialValue) {
        Arrays.fill(values, initialValue);
    }

    public void setX (int x, int value) {
        Arrays.fill(values, x * dy, (x + 1) * dy, value);
    }

    public void setY (int y, int value) {
        int index = y;
        while (index < values.length) {
            values[index] = value;
            index += dy;
        }
    }

    public int get (int x, int y) {
        return values[x * dy + y];
    }

    public void set (int x, int y, int value) {
        values[x * dy + y] = value;
    }

    public boolean setIfLess (int x, int y, int value) {
        int index = x * dy + y;
        if (values[index] > value) {
            values[index] = value;
            return true;
        }
        return false;
    }

    public void adjust (int x, int y, int amount) {
        int index = x * dy + y;
        values[index] += amount;
    }

}
