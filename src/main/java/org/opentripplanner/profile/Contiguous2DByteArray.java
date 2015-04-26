package org.opentripplanner.profile;

import java.io.Serializable;

public class Contiguous2DByteArray implements Serializable {

    final int dx, dy;
    final byte[] values;

    public Contiguous2DByteArray(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
        values = new byte[dx * dy];
        System.out.println("allocated " + (dx * dy / 1024) + "kbytes");
    }

    public byte get (int x, int y) {
        return values[x * dy + y];
    }

    public void set (int x, int y, byte value) {
        values[x * dy + y] = value;
    }

    public boolean setIfLess (int x, int y, byte value) {
        int index = x * dy + y;
        if (values[index] == 0 || values[index] > value) {
            values[index] = value;
            return true;
        }
        return false;
    }

    public void adjust (int x, int y, byte amount) {
        int index = x * dy + y;
        values[index] += amount;
    }

}
