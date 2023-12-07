package net.sourceforge.jFuzzyLogic;

import net.sourceforge.jFuzzyLogic.demo.tipper.TipperAnimation;
import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
import net.sourceforge.jFuzzyLogic.rule.Rule;
import net.sourceforge.jFuzzyLogic.rule.RuleBlock;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import org.antlr.runtime.RecognitionException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main jFuzzyLogic class
 * 
 * Parses an FCL file or shows a demo.
 * 
 * @author pablocingolani
 */
public class JFuzzyLogic {

	// Version info
	public static final String SOFTWARE_NAME = "JFuzzyLogic";
	public static final String REVISION = "";
	public static final String BUILD = "2022-05-12";
	public static final String VERSION_MAJOR = "4.0";
	public static final String VERSION_SHORT = VERSION_MAJOR + REVISION;
	public static final String VERSION_NO_NAME = VERSION_SHORT + " (build " + BUILD + "), by " + Pcingola.BY;
	public static final String VERSION = SOFTWARE_NAME + " " + VERSION_NO_NAME;
	private static final String COMMA_DELIMITER = ";";

	public static boolean debug = false;

	String args[];
	FIS fis;

	public static void main(String[] args) throws Exception {
		JFuzzyLogic jFuzzyLogic = new JFuzzyLogic(args);
		jFuzzyLogic.run();
	}

	public JFuzzyLogic(String args[]) {
		this.args = args;
	}

	/**
	 * Compile an FCL program into C++
	 */
	void compile(String fileName) {
		load(fileName); // Read FIS
		System.out.println(fis.toStringCpp()); // Show C++ code

		// Dump C++ code to a file (debug)
		if (debug) {
			String cfile = Gpr.HOME + "/x.cpp";
			Gpr.debug("Writing to file " + cfile);
			Gpr.toFile(cfile, fis.toStringCpp());
		}
	}

	/**
	 * Compile an FCL program into JavaScript
	 */
	void compileJS(String fileName) {
		load(fileName); // Read FIS
		System.out.println(fis.toStringJS()); // Show JS code

		// Dump JS code to a file (debug)
		if (debug) {
			String jsfile = Gpr.HOME + "/x.js";
			Gpr.debug("Writing to file " + jsfile);
			Gpr.toFile(jsfile, fis.toStringJS());
		}
	}

	/**
	 * Run demo
	 */
	void demo() {
		// Run demo
		System.err.println("Running demo");
		try {
			TipperAnimation.main(args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	double classifyBinary(int argNum) {
		double result = -1.0;
		List<List<String>> records = new ArrayList<>();
		String testFileName = args[argNum++];
		try (Scanner scanner = new Scanner(new File(testFileName))) {
			while (scanner.hasNextLine()) {
				records.add(getRecordFromLine(scanner.nextLine()));
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		List<Integer> expected = new ArrayList<>();
		List<Integer> predicted = new ArrayList<>();
		int tp = 0, tn = 0, fp = 0, fn = 0;

		String fileName = args[argNum++];
		load(fileName);

		double decisionPoint = Double.parseDouble(args[argNum++]);
		String decisionType = args[argNum++];

		for (List<String> record : records) {
			String expectedValue = record.get(record.size() - 1);
			expected.add(Integer.valueOf(expectedValue));

			//---
			// Assign values (parse command line)
			//---
			for (FunctionBlock fb : fis) {
				int k = 0;
				for (Variable var : fb.variablesSorted()) {
					if (var.isInput()) {
						var.setValue(Gpr.parseDoubleSafe(record.get(k)));
						k++;
					}
				}
			}

			//---
			// Evaluate and show results
			//---
			fis.evaluate(); // Evaluate

			// Show values
			for (FunctionBlock fb : fis) {
				// Show variables
				for (Variable var : fb.variablesSorted()) {
					if (var.isOutput()) {
						double res = var.getValue();
						switch (decisionType) {
							case "gt":
								if (res > decisionPoint) {
									predicted.add(1);
								} else {
									predicted.add(0);
								}
								break;
							case "lt":
								if (res < decisionPoint) {
									predicted.add(1);
								} else {
									predicted.add(0);
								}
								break;
							case "geq":
								if (res >= decisionPoint) {
									predicted.add(1);
								} else {
									predicted.add(0);
								}
								break;
							case "leq":
								if (res <= decisionPoint) {
									predicted.add(1);
								} else {
									predicted.add(0);
								}
								break;
						}
					}
				}
			}

			if (expected.get(expected.size() - 1).equals(predicted.get(predicted.size() - 1))) {
				if (predicted.get(predicted.size() - 1).equals(1)) {
					tp = tp + 1;
				} else {
					tn = tn + 1;
				}
			} else {
				if (predicted.get(predicted.size() - 1).equals(1)) {
					fp = fp + 1;
				} else {
					fn = fn + 1;
				}
			}
		}

		double prec = (tp * 1.0) / (tp + fp);
		double rec = (tp * 1.0) / (tp + fn);
		result = 2.0 * ((prec * rec * 1.0) / (prec + rec));

		try {
			saveCSV(testFileName.replace(".csv", "_out.csv"), records, predicted);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		System.out.println("F1-score is: " + result);

		return result;
	}

	public void saveCSV(String fileName, List<List<String>> records, List<Integer> colToAdd) throws IOException {
		File csvOutputFile = new File(fileName);
		csvOutputFile.createNewFile();
		try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
			for (int k = 0; k < records.size(); k++) {
				List<String> record = records.get(k);
				StringBuilder stringBuilder = new StringBuilder();

				for (String recordValue : record) {
					stringBuilder.append(recordValue).append(COMMA_DELIMITER);
				}

				stringBuilder.append(colToAdd.get(k)).append("\n");

				pw.printf(stringBuilder.toString());
			}
		}
	}

	private List<String> getRecordFromLine(String line) {
		List<String> values = new ArrayList<>();
		try (Scanner rowScanner = new Scanner(line)) {
			rowScanner.useDelimiter(COMMA_DELIMITER);
			while (rowScanner.hasNext()) {
				values.add(rowScanner.next());
			}
		}
		return values;
	}

	/**
	 * Evaluate an FCL file
	 */
	int evaluate(int argNum) {
		// Parse a file and show membership functions
		String fileName = args[argNum++];
		load(fileName);

		//---
		// Assign values (parse command line)
		//---
		boolean missing = false;
		for (FunctionBlock fb : fis) {
			for (Variable var : fb.variablesSorted()) {
				if (var.isInput()) {
					if (argNum < args.length) var.setValue(Gpr.parseDoubleSafe(args[argNum++]));
					else {
						System.err.println("Missing argument '" + fb.getName() + "." + var.getName() + "' (input variable '" + var.getName() + "' in FunctionBlock '" + fb.getName() + "').");
						missing = true;
					}
				}
			}
		}
		if (missing) usage("Missing input variables values.");

		//---
		// Evaluate and show results
		//---
		fis.evaluate(); // Evaluate

		// Show values
		for (FunctionBlock fb : fis) {
			System.out.println("FUNCITON_BLOCK " + fb.getName());

			// Show variables
			for (Variable var : fb.variablesSorted()) {
				if (var.isInput()) System.out.println(String.format("\tVAR_INPUT \t%10s = %6f", var.getName(), var.getValue()));
				else if (var.isOutput()) System.out.println(String.format("\tVAR_OUTPUT\t%10s = %6f", var.getName(), var.getValue()));
			}

			// Show rules
			for (RuleBlock rb : fb) {
				System.out.println("\tRULE_BLOCK " + rb.getName());
				System.out.println("\t\tSupport  \tRule name \tRule");
				for (Rule r : rb) {
					System.out.println(String.format("\t\t%6f\t%-10s\t%s", r.getDegreeOfSupport(), r.getName(), r.toStringFcl()));
				}
			}
		}

		// Show charts
		JFuzzyChart.get().chart(fis);

		return argNum;
	}

	public FIS getFis() {
		return fis;
	}

	/**
	 * Load FIS form an FCL file
	 */
	FIS load(String fclFileName) {
		try {
			String fcl = Gpr.readFile(fclFileName);
			fis = FIS.createFromString(fcl, true);
		} catch (RecognitionException e) {
			throw new RuntimeException(e);
		}
		return fis;
	}

	/**
	 * Run program
	 */
	public void run() {
		// Show start message
		System.err.println("jFuzzyLogic version " + VERSION + ".\n");

		// Check args
		if (args.length <= 0) usage(null);

		// Options
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			// Parse command line options
			if (arg.equals("demo")) {
				// Show demo and exit
				demo();
				return;
			} else if (arg.equals("-h") || arg.equals("-help") || arg.equals("--help")) {
				// Show help
				usage(null);
				return;
			} else if (arg.equals("-c")) {
				// Sanity check
				String fileName = args[++i];
				compile(fileName);
				return;
			} else if (arg.equals("-j")) {
				// Sanity check
				String fileName = args[++i];
				compileJS(fileName);
				return;
			} else if (arg.equals("-e")) {
				evaluate(i + 1);
				return;
			} else if (arg.equals("-b")) {
				classifyBinary(i + 1);
				return;
			} else if (arg.equalsIgnoreCase("-noCharts")) {
				// Do not use chart classes
				JFuzzyChart.UseMockClass = true;
			} else {
				String fileName = args[i];
				show(fileName);
			}
		}
	}

	/**
	 * Show membership functions
	 */
	void show(String fileName) {
		load(fileName); // Parse a file and show membership functions	
		JFuzzyChart.get().chart(fis);// Show variables for each function block
		System.out.println(fis); // Show code
	}

	/**
	 * Show usage message
	 */
	void usage(String message) {
		System.err.println(VERSION);

		if (message != null) System.err.println("Error: " + message + "\n");

		System.err.println("Usage: java -jar jFuzzyLogic.jar [-noCharts] [{-e|-c}] file.fcl [in_1 ... in_N]");
		System.err.println("Options:");
		System.err.println("\t file.fcl                                            : Load FCL file and show membership functions (default, when no option is provided).");
		System.err.println("\t-c file.fcl                                          : Compile. Generate C++ code from FCL file (to STDOUT)");
		System.err.println("\t-j file.fcl                                          : Compile. Generate JavaScript code from FCL file (to STDOUT)");
		System.err.println("\t-e file.fcl in_1 in_2 ... in_N                       : Evaluate. Load FCL file, assign inputs i_1, i_2, ..., i_n and evaluate (variables sorted alphabetically).");
		System.err.println("\t-b test.csv file.fcl decisionValue decisionType      : Evaluate for CSV file - last column must be for \"expected values\"; decisionType: gt; lt; geq; leq; decisionValue: IF calculatedValue decisionType decisionValue THEN return 1; ELSE return 0;");
		System.err.println("\t-noCharts                                            : Use a mock class for charts. This is used when not compiled using JFreeCharts.");
		System.err.println("\tdemo                                                 : Run a demo example (tipper.fcl)");
		System.exit(1);
	}
}
