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

package org.opentripplanner.common.geometry;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Variable-length integer encoding. This optimize integer storage when most of the values are
 * small, but few of them can be quite large (as in a geometry). Adapted Dlugosz scheme to support
 * signed int whose average are around 0.
 * 
 * See Dlugosz' variable-length integer encoding (http://www.dlugosz.com/ZIP2/VLI.html).
 * 
 * @author laurent
 * 
 */
public class DlugoszVarLenIntPacker {

    public static byte[] pack(int[] arr) {
        if (arr == null)
            return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(arr.length);
        for (int i : arr) {
            if (i >= -64 && i <= 63) {
                // 0xxx xxxx -> 7 bits value
                // i+64 between 0 and 127, 7 bits
                int ui = i + 64;
                baos.write(ui);
            } else if (i >= -8192 && i <= 8191) {
                // 10xx xxxx + 8 -> 14 bits value
                // i+8192 between 0 and 16383
                int ui = i + 8192;
                baos.write(0x80 | (ui >> 8)); // 6b MSB
                baos.write(ui & 0xFF); // 8b LSB
            } else if (i >= -1048576 && i <= 1048575) {
                // 110 xxxx + 2x8 -> 21 bits value
                // i + 1048576 between 0 and 2097151
                int ui = i + 1048576;
                baos.write(0xC0 | (ui >> 16)); // 5b MSB
                baos.write((ui >> 8) & 0xFF); // 8b
                baos.write(ui & 0xFF); // 8b
            } else if (i >= -67108864 && i <= 67108863) {
                // 1110 0xxx + 3x8 -> 27 bits value
                // i + 67108864 between 0 and 134217727
                int ui = i + 67108864;
                baos.write(0xE0 | (ui >> 24)); // 3b MSB
                baos.write((ui >> 16) & 0xFF); // 8b
                baos.write((ui >> 8) & 0xFF); // 8b
                baos.write(ui & 0xFF); // 8b
            } else { // int can't have more than 32 bits
                // 1110 1xxx + 4x8 -> 35 bits value
                // i + 0x80000000 fits in 35 bits for sure
                long ui = (long) i + 2147483648L;
                baos.write((int) (0xE8 | (ui >> 32))); // 3b MSB
                baos.write((int) ((ui >> 24) & 0xFF)); // 8b
                baos.write((int) ((ui >> 16) & 0xFF)); // 8b
                baos.write((int) ((ui >> 8) & 0xFF)); // 8b
                baos.write((int) (ui & 0xFF)); // 8b
            }
        }
        return baos.toByteArray();
    }

    public static int[] unpack(byte[] arr) {
        if (arr == null)
            return null;
        List<Integer> retval = new ArrayList<Integer>(arr.length);
        int i = 0;
        while (i < arr.length) {
            int v1 = arr[i] & 0xFF;
            i++;
            if ((v1 & 0x80) == 0x00) {
                // 0xxx xxxx -> 7 bits value
                int sv = (v1 & 0x7F) - 64;
                retval.add(sv);
            } else if ((v1 & 0xC0) == 0x80) {
                // 10xx xxxx + 8 -> 14 bits value
                int sv = ((v1 & 0x3F) << 8) + (arr[i] & 0xFF) - 8192;
                i++;
                retval.add(sv);
            } else if ((v1 & 0xE0) == 0xC0) {
                // 110 xxxx + 2x8 -> 21 bits value
                int sv = ((v1 & 0x1F) << 16) + ((arr[i] & 0xFF) << 8) + (arr[i + 1] & 0xFF)
                        - 1048576;
                i += 2;
                retval.add(sv);
            } else if ((v1 & 0xF8) == 0xE0) {
                // 1110 0xxx + 3x8 -> 27 bits value
                int sv = ((v1 & 0x1F) << 24) + ((arr[i] & 0xFF) << 16) + ((arr[i + 1] & 0xFF) << 8)
                        + (arr[i + 2] & 0xFF) - 67108864;
                i += 3;
                retval.add(sv);
            } else {
                // 1110 1xxx + 4x8 -> 35 bits value
                long sv = (((long) v1 & 0x1F) << 32) + ((arr[i] & 0xFF) << 24)
                        + ((arr[i + 1] & 0xFF) << 16) + ((arr[i + 2] & 0xFF) << 8)
                        + (arr[i + 3] & 0xFF) - 2147483648L;
                i += 4;
                retval.add((int) sv);
            }
        }
        int[] bufret = new int[retval.size()];
        i = 0;
        for (int v : retval) {
            bufret[i++] = v;
        }
        return bufret;
    }
}
