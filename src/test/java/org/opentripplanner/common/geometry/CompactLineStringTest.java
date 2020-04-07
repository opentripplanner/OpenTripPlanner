package org.opentripplanner.common.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

public class CompactLineStringTest extends TestCase {

    @Test
    public final void testCompactString() {

        GeometryFactory gf = new GeometryFactory();

        List<Coordinate> c = new ArrayList<Coordinate>();
        double x0 = 1.111111111;
        double y0 = 0.123456789;
        double x1 = 2.0;
        double y1 = 0.0;
        c.add(new Coordinate(x0, y0));
        c.add(new Coordinate(x1, y1));
        LineString ls = gf.createLineString(c.toArray(new Coordinate[0]));
        int[] coords = CompactLineString.compactLineString(x0, y0, x1, y1, ls, false);
        assertTrue(coords == CompactLineString.STRAIGHT_LINE); // ==, not equals
        LineString ls2 = CompactLineString.uncompactLineString(x0, y0, x1, y1, coords, false);
        assertTrue(ls.equalsExact(ls2, 0.00000015));
        byte[] packedCoords = CompactLineString.compackLineString(x0, y0, x1, y1, ls, false);
        assertTrue(packedCoords == CompactLineString.STRAIGHT_LINE_PACKED); // ==, not equals
        ls2 = CompactLineString.uncompackLineString(x0, y0, x1, y1, packedCoords, false);
        assertTrue(ls.equalsExact(ls2, 0.00000015));

        c.clear();
        c.add(new Coordinate(x0, y0));
        c.add(new Coordinate(-179.99, 1.12345));
        c.add(new Coordinate(179.99, 1.12345));
        c.add(new Coordinate(x1, y1));
        ls = gf.createLineString(c.toArray(new Coordinate[0]));
        coords = CompactLineString.compactLineString(x0, y0, x1, y1, ls, false);
        assertTrue(coords != CompactLineString.STRAIGHT_LINE);
        ls2 = CompactLineString.uncompactLineString(x0, y0, x1, y1, coords, false);
        assertTrue(ls.equalsExact(ls2, 0.00000015));
        packedCoords = CompactLineString.compackLineString(x0, y0, x1, y1, ls, false);
        assertTrue(packedCoords != CompactLineString.STRAIGHT_LINE_PACKED);
        ls2 = CompactLineString.uncompackLineString(x0, y0, x1, y1, packedCoords, false);
        assertTrue(ls.equalsExact(ls2, 0.00000015));

        // Test reverse mode
        LineString lsi = (LineString) ls.reverse(); // The expected output
        int[] coords2 = CompactLineString.compactLineString(x1, y1, x0, y0, ls, true);
        assertTrue(coords2 != CompactLineString.STRAIGHT_LINE);
        assertEquals(coords.length, coords2.length);
        for (int i = 0; i < coords.length; i++)
            assertEquals(coords[i], coords2[i]);
        ls2 = CompactLineString.uncompactLineString(x1, y1, x0, y0, coords2, true);
        assertTrue(lsi.equalsExact(ls2, 0.00000015));
        LineString ls3 = CompactLineString.uncompactLineString(x1, y1, x0, y0, coords, true);
        assertTrue(lsi.equalsExact(ls3, 0.00000015));
        byte[] packedCoords2 = CompactLineString.compackLineString(x1, y1, x0, y0, ls, true);
        assertTrue(packedCoords2 != CompactLineString.STRAIGHT_LINE_PACKED);
        assertEquals(packedCoords.length, packedCoords2.length);
        for (int i = 0; i < packedCoords.length; i++)
            assertEquals(packedCoords[i], packedCoords2[i]);
        ls2 = CompactLineString.uncompackLineString(x1, y1, x0, y0, packedCoords2, true);
        assertTrue(lsi.equalsExact(ls2, 0.00000015));
        ls3 = CompactLineString.uncompackLineString(x1, y1, x0, y0, packedCoords, true);
        assertTrue(lsi.equalsExact(ls2, 0.00000015));
    }

    @Test
    public final void testDlugoszVarLenIntPacker() {

        packTest(new int[] {}, 0);
        packTest(new int[] { 0 }, 1);
        packTest(new int[] { 63 }, 1);
        packTest(new int[] { -64 }, 1);
        packTest(new int[] { 64 }, 2);
        packTest(new int[] { -65 }, 2);
        packTest(new int[] { -8192 }, 2);
        packTest(new int[] { -8193 }, 3);
        packTest(new int[] { 8191 }, 2);
        packTest(new int[] { 8192 }, 3);
        packTest(new int[] { -1048576 }, 3);
        packTest(new int[] { -1048577 }, 4);
        packTest(new int[] { 1048575 }, 3);
        packTest(new int[] { 1048576 }, 4);
        packTest(new int[] { -67108864 }, 4);
        packTest(new int[] { -67108865 }, 5);
        packTest(new int[] { 67108863 }, 4);
        packTest(new int[] { 67108864 }, 5);
        packTest(new int[] { Integer.MAX_VALUE }, 5);
        packTest(new int[] { Integer.MIN_VALUE }, 5);

        packTest(new int[] { 0, 0 }, 2);
        packTest(new int[] { 0, 0, 0 }, 3);

        packTest(new int[] { 8100, 8200, 8300 }, 8);
    }

    private void packTest(int[] arr, int expectedPackedLen) {
        byte[] packed = DlugoszVarLenIntPacker.pack(arr);
        System.out.println("Unpacked: " + Arrays.toString(arr) + " -> packed: "
                + unsignedCharString(packed));
        assertEquals(expectedPackedLen, packed.length);
        int[] unpacked = DlugoszVarLenIntPacker.unpack(packed);
        assertTrue(Arrays.equals(arr, unpacked));
    }

    private String unsignedCharString(byte[] data) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < data.length; i++) {
            sb.append(String.format("%02X", data[i] & 0xFF));
            if (i < data.length - 1)
                sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
