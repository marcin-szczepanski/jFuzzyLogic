package net.sourceforge.jFuzzyLogic;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.Ignore;

@Ignore
public class TestSuiteAll {

	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Stats
		suite.addTestSuite(TestCaseJfuzzy.class);
		suite.addTestSuite(TestCaseTipper.class);
		suite.addTestSuite(TestCaseCommandLine.class);

		return suite;
	}
}
