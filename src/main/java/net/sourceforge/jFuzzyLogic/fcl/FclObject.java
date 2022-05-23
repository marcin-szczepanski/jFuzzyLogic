package net.sourceforge.jFuzzyLogic.fcl;

import net.sourceforge.jFuzzyLogic.CompileCpp;
import net.sourceforge.jFuzzyLogic.CompileJS;

/**
 * The root of all FCL objects
 * 
 * @author pcingola
 *
 */
public abstract class FclObject implements CompileCpp, CompileJS {

	@Override
	public String toString() {
		return toStringFcl();
	}

	@Override
	public String toStringCpp() {
		return "// " + this.getClass().getName();
	}

	@Override
	public String toStringJS() {
		return "// " + this.getClass().getName();
	}

	public abstract String toStringFcl();
}
