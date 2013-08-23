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

package org.opentripplanner.updater;

/**
 * A cyclic process that periodically update a graph.
 * 
 * Instance of this interface will be added to a round-robin scheduler system which ensure that no
 * two GraphUpdaterRunnable will run at the same time ever. So client can safely assume that no
 * locking is needed between any two different concurrent GraphUpdaterRunnable instances.
 * 
 */
@Deprecated
public interface GraphUpdaterRunnable {

    public void setup();

    public void run();

    public void teardown();
}
