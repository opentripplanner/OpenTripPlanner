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

import junit.framework.TestCase;

public class BitSetUtilsTest extends TestCase {

    public void testBitSetUtils() {

        byte bflags = 0;
        short sflags = 0;
        int iflags = 0;
        long lflags = 0L;

        for (int i = 0; i < 8; i++) {
            assertEquals(false, BitSetUtils.get(bflags, i));
            bflags = BitSetUtils.set(bflags, i, true);
            assertEquals(true, BitSetUtils.get(bflags, i));
            bflags = BitSetUtils.set(bflags, i, false);
            assertEquals(false, BitSetUtils.get(bflags, i));
        }
        for (int i = 0; i < 16; i++) {
            assertEquals(false, BitSetUtils.get(sflags, i));
            sflags = BitSetUtils.set(sflags, i, true);
            assertEquals(true, BitSetUtils.get(sflags, i));
            sflags = BitSetUtils.set(sflags, i, false);
            assertEquals(false, BitSetUtils.get(sflags, i));
        }
        for (int i = 0; i < 32; i++) {
            assertEquals(false, BitSetUtils.get(iflags, i));
            iflags = BitSetUtils.set(iflags, i, true);
            assertEquals(true, BitSetUtils.get(iflags, i));
            iflags = BitSetUtils.set(iflags, i, false);
            assertEquals(false, BitSetUtils.get(iflags, i));
        }
        for (int i = 0; i < 64; i++) {
            assertEquals(false, BitSetUtils.get(lflags, i));
            lflags = BitSetUtils.set(lflags, i, true);
            assertEquals(true, BitSetUtils.get(lflags, i));
            lflags = BitSetUtils.set(lflags, i, false);
            assertEquals(false, BitSetUtils.get(lflags, i));
        }
    }
}
