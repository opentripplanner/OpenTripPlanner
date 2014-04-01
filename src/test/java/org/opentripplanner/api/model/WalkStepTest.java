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

package org.opentripplanner.api.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class WalkStepTest {
	private static final String NO_PARENS_STREET_NAME = "a normal name";
	private static final String START_WITH_PARENS_STREET_NAME = "(start with paren)";
	private static final String PARENS_STREET_NAME = "a normal name (paren)";
	
	private WalkStep step;
	
	@Before
	public void init() {
		step = new WalkStep();
	}

	@Test
	public void testNameNoParensWithNoParensName() {
		step.streetName = NO_PARENS_STREET_NAME;
		assertEquals(NO_PARENS_STREET_NAME, step.streetNameNoParens());
	}
	
	@Test 
	public void testNameNoParensWithNameStaringWithParens() {
		step.streetName = START_WITH_PARENS_STREET_NAME;
		assertEquals(START_WITH_PARENS_STREET_NAME, step.streetNameNoParens());
	}
	
	@Test 
	public void testNameNoParensWithNameWithParens() {
		step.streetName = PARENS_STREET_NAME;
		assertEquals(NO_PARENS_STREET_NAME, step.streetNameNoParens());
	}

}
