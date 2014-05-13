package com.google.code.tvrenamer.controller.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class StringUtilsTest {

	@Test
	public void testSanitiseTitleBackslash() {
		assertEquals(StringUtils.sanitiseTitle("Test\\"), "Test-");
	}
	
	@Test
	public void testSanitiseTitleForwardslash() {
		assertEquals(StringUtils.sanitiseTitle("Test/"), "Test-");
	}
	
	@Test
	public void testSanitiseTitleColon() {
		assertEquals(StringUtils.sanitiseTitle("Test:"), "Test -");
	}
	
	@Test
	public void testSanitiseTitlePipe() {
		assertEquals(StringUtils.sanitiseTitle("Test|"), "Test-");
	}
	
	@Test
	public void testSanitiseTitleAsterisk() {
		assertEquals(StringUtils.sanitiseTitle("Test*"), "Test");
	}
	
	@Test
	public void testSanitiseTitleQuestionMark() {
		assertEquals(StringUtils.sanitiseTitle("Test?"), "Test");
	}
	
	@Test
	public void testSanitiseTitleGreaterThan() {
		assertEquals(StringUtils.sanitiseTitle("Test>"), "Test");
	}
	
	@Test
	public void testSanitiseTitleLessThan() {
		assertEquals(StringUtils.sanitiseTitle("Test<"), "Test");
	}
	
	@Test
	public void testSanitiseTitleDoubleQuote() {
		assertEquals(StringUtils.sanitiseTitle("Test\""), "Test'");
	}

	@Test
	public void testSanitiseTitleTilde() {
		assertEquals(StringUtils.sanitiseTitle("Test`"), "Test'");
	}
}
