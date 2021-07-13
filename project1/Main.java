import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * @author Atilla Türkmen
 */

public class Main {

	/**
	 * @param args One argument which is the path of input file
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {

		// Path to input and output files
		String inputPath = args[0];
		String outputPath = inputPath.substring(0, inputPath.length() - 3) + ".ll";

		// Read mylang input
		Scanner input = new Scanner(new File(inputPath));

		// Tokenize line by line
		ArrayList<ArrayList<String>> lines = new ArrayList<ArrayList<String>>();

		while (input.hasNextLine()) {

			// Tokenize symbols
			StringTokenizer st = new StringTokenizer(input.nextLine(), "+-/*()={}# 	,", true);

			// Remove white line
			ArrayList<String> tokens = new ArrayList<String>();
			while (st.hasMoreTokens()) {
				String next = st.nextToken();
				if (next.matches("\\s|\\t"))
					continue;
				tokens.add(next);
			}

			// Add to lines list
			lines.add(tokens);
		}

		input.close();

		// Produce output with parser
		Parser parser = new Parser(outputPath);
		parser.printStartingLines();
		parser.initializeVars(lines);
		parser.produceOutput(lines);
		parser.printEndingLines();
	}

}
