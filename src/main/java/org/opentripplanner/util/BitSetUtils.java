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
package org.opentripplanner.util;

/**
 * A compact bit set utility class. It rely on the client to store the bit set himself (either as a
 * byte, short, int or long, depending on the amount of bits needed), to prevent having to store a
 * reference and create another heavy object instance such as with the BitSet class.
 * 
 * Usage is rather simple:
 * 
 * <pre>
 * 
 * short flags; // or int, long, byte
 * 
 * public boolean isFooBar() {
 *     return BitSetUtils.get(flags, FOOBAR_INDEX);
 * }
 * 
 * public void setFooBar(boolean foobar) {
 *     flags = BitSetUtils.set(flags, FOOBAR_INDEX, foobar);
 * }
 * 
 * </pre>
 * 
 * @author laurent
 * 
 */
public final class BitSetUtils {

    public final static boolean get(byte bitset, int index) {
        return (bitset & (1 << index)) != 0;
    }

    public final static byte set(byte bitset, int index, boolean value) {
        if (value)
            bitset |= (1 << index);
        else
            bitset &= ~(1 << index);
        return bitset;
    }

    public final static boolean get(short bitset, int index) {
        return (bitset & (1 << index)) != 0;
    }

    public final static short set(short bitset, int index, boolean value) {
        if (value)
            bitset |= (1 << index);
        else
            bitset &= ~(1 << index);
        return bitset;
    }

    public final static boolean get(int bitset, int index) {
        return (bitset & (1 << index)) != 0;
    }

    public final static int set(int bitset, int index, boolean value) {
        if (value)
            bitset |= (1 << index);
        else
            bitset &= ~(1 << index);
        return bitset;
    }

    public final static boolean get(long bitset, int index) {
        return (bitset & (1L << index)) != 0;
    }

    public final static long set(long bitset, int index, boolean value) {
        if (value)
            bitset |= (1L << index);
        else
            bitset &= ~(1L << index);
        return bitset;
    }

}