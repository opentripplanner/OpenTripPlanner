package org.opentripplanner.api.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiWalkStepTest {
	private static final String NO_PARENS_STREET_NAME = "a normal name";
	private static final String START_WITH_PARENS_STREET_NAME = "(start with paren)";
	private static final String PARENS_STREET_NAME = "a normal name (paren)";
	
	private ApiWalkStep step;
	
	@Before
	public void init() {
		step = new ApiWalkStep();
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
