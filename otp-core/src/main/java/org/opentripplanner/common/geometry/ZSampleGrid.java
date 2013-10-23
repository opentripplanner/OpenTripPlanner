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

interface ZSamplePoint<TZ, U> {
    public int getX();

    public int getY();

    public TZ getZ();

    public void setZ(TZ z);

    public U getU();

    public void setU(U u);

    public ZSamplePoint<TZ, U> up();

    public ZSamplePoint<TZ, U> down();

    public ZSamplePoint<TZ, U> right();

    public ZSamplePoint<TZ, U> left();

}

/**
 * A generic indexed grid of Z samples.
 * 
 * @author laurent
 */
public interface ZSampleGrid<TZ, U> extends Iterable<ZSamplePoint<TZ, U>> {

    public ZSamplePoint<TZ, U> getOrCreate(int x, int y);
    
    public int size();

}