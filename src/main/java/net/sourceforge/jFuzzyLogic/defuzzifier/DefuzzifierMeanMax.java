package net.sourceforge.jFuzzyLogic.defuzzifier;

import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 * Mean of maxima defuzzyfier
 * @author pcingola@users.sourceforge.net
 */
public class DefuzzifierMeanMax extends DefuzzifierContinuous {

	public DefuzzifierMeanMax(Variable variable) {
		super(variable);
	}

	/** Deffuzification function */
	@Override
	public double defuzzify() {
		/**
		 * Fixes:
		 * @author mdsflyboy
		 */

		double max = 0, maxX = 0;

		// Calculate max
		for (double value : values) {
			if (value >= max)
				max = value;
		}

		// No max? => this variable has no active antecedent
		if (max <= 0) return Double.NaN;

		// Calculate mean of max
		boolean isFirstMax = false;
		int startOfMax = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i] == max) {
				if (!isFirstMax){
					isFirstMax = true;
					startOfMax = i;
				}
				maxX = i;
			}
		}

		// Return mean of max
		int middleI = (int) ((maxX + startOfMax) / 2);
		return min + stepSize * middleI;
	}

	@Override
	public String toStringFcl() {
		return "METHOD : MM;";
	}
}
