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
