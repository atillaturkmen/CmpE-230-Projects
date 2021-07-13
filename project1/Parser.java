import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

/**
 * @author Atilla Türkmen
 */

public class Parser {

	/**
	 * Writes to output file
	 */
	private PrintWriter writer;
	/**
	 * Keeps track of current line number for syntax error messages
	 */
	private int lineNo = 0;
	/**
	 * Number of temporary variables. Used for naming.
	 */
	private int tempNo = 0;
	/**
	 * Number of if statements. Used for labeling.
	 */
	private int ifNo = 0;
	/**
	 * Number of while statements. Used for labeling.
	 */
	private int whileNo = 0;
	/**
	 * Number of choose statements. Used for labeling.
	 */
	private int chooseNo = 0;
	/**
	 * Holds all variable names.
	 */
	private HashSet<String> vars = new HashSet<String>();
	/**
	 * Path of the output file, needed for syntax error function.
	 */
	private String outputPath;

	/**
	 * Constructor of the Parser class, initializes printwriter at given output path.
	 * 
	 * @param writer     Writer that writes to output file
	 * @param outputPath Path of the output file, needed for syntax error function
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 */
	public Parser(String outputPath) throws FileNotFoundException, UnsupportedEncodingException {
		this.writer = new PrintWriter(outputPath, "UTF-8");
		this.outputPath = outputPath;
	}

	/**
	 * Prints the starting LLVM lines
	 */
	public void printStartingLines() {
		writer.println("; ModuleID = 'mylang2ir'");
		writer.println("declare i32 @printf(i8*, ...)");
		writer.println("@print.str = constant [4 x i8] c\"%d\\0A\\00\"");
		writer.println("@error.str = constant [23 x i8] c\"Line %d: syntax error\\0A\\00\"");
		writer.println("define i32 @main() {");
	}

	/**
	 * Prints ending of LLVM file and closes the writer
	 */
	public void printEndingLines() {
		writer.println("ret i32 0");
		writer.println("}");
		writer.close();
	}

	/**
	 * Allocate and store 0 in future variables
	 * 
	 * @param lines List of lines to search variable names
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public void initializeVars(ArrayList<ArrayList<String>> lines)
			throws FileNotFoundException, UnsupportedEncodingException {
		for (int i = 0; i < lines.size(); i++) {
			ArrayList<String> line = lines.get(i);
			for (int j = 0; j < line.size(); j++) {
				String token = line.get(j);
				if (token.equals("#"))
					break;
				if (acceptableVarName(token)) {
					checkVar(token);
				}
			}
		}
	}

	/**
	 * Takes input written in myLang, prints VM code that corresponds to it.
	 * 
	 * @param lines List of lines, every line is a list of tokens
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public void produceOutput(ArrayList<ArrayList<String>> lines)
			throws FileNotFoundException, UnsupportedEncodingException {
		for (int j = 0; j < lines.size(); j++) {
			ArrayList<String> tokens = lines.get(j);

			// Delete tokens after "#"
			for (int i = 0; i < tokens.size(); i++) {
				if (tokens.get(i).equals("#")) {
					tokens = new ArrayList<String>(tokens.subList(0, i));
					break;
				}
			}

			// Number of tokens in this line
			int nofTokens = tokens.size();

			// continue if line is empty
			if (nofTokens == 0 || tokens.get(0).equals("")) {
				lineNo++;
				continue;
			}

			// a line must consist of at least 3 tokens if the program has not reached any
			// if or while conditions
			if (nofTokens < 3) {
				// System.out.println("Less than 3 tokens on one line");
				syntaxError();
			}

			checkParenthesis(tokens);

			// print function
			if (tokens.get(0).equals("print")) {
				// parenthesis expected
				if (!tokens.get(1).equals("(") || !tokens.get(nofTokens - 1).equals(")")) {
					// System.out.println("parantez expected");
					syntaxError();
				}
				List<String> expression = tokens.subList(2, nofTokens - 1);
				String result = computeExpression(expression);
				writer.println(
						"call i32 (i8*, ...)* @printf(i8* getelementptr ([4 x i8]* @print.str, i32 0, i32 0), i32 "
								+ result + " )");
			}

			// if statement
			else if (tokens.get(0).equals("if")) {
				tokens.remove(0);
				// take the expression inside parenthesis
				List<String> expression = evaluateInsideParenthesis(tokens);
				// evaluate the result and print branching LLVM code
				String result = computeExpression(expression);
				writer.println("%t" + ++tempNo + " = icmp ne i32 " + result + ", 0");
				writer.println("br i1 %t" + tempNo + ", label %ifbody" + ifNo + ", label %ifend" + ifNo);
				writer.println("ifbody" + ifNo + ":");
				// take lines between two curly brackets and call this function on those lines
				j = evaluateInsideCurlyBrackets(lines, j);
				writer.println("br label %ifend" + ifNo);
				writer.println("ifend" + ifNo++ + ":");
			}

			// while statement
			else if (tokens.get(0).equals("while")) {
				tokens.remove(0);
				// take the expression inside parenthesis
				List<String> expression = evaluateInsideParenthesis(tokens);
				// evaluate the result and print branching LLVM code
				writer.println("br label %whcond" + whileNo);
				writer.println("whcond" + whileNo + ":");
				String result = computeExpression(expression);
				writer.println("%t" + ++tempNo + " = icmp ne i32 " + result + ", 0");
				writer.println("br i1 %t" + tempNo + ", label %whbody" + whileNo + ", label %whend" + whileNo);
				writer.println("whbody" + whileNo + ":");
				// take lines between two curly brackets and call this function on those lines
				j = evaluateInsideCurlyBrackets(lines, j);
				writer.println("br label %whcond" + whileNo);
				writer.println("whend" + whileNo++ + ":");
			}

			// assignment
			else if (tokens.get(1).equals("=")) {
				// read left side of assignment
				String leftSide = tokens.get(0);
				checkVar(leftSide);
				List<String> rightSide = tokens.subList(2, nofTokens);
				String result = computeExpression(rightSide);
				writer.println("store i32 " + result + ", i32* %" + leftSide + "r");
			}

			// Syntax error
			else {
				// System.out.println("unexpected token at the begining of line");
				syntaxError();
			}

			lineNo++;
		}
	}

	/**
	 * Produces the LLVM code that computes the expression if it is an operation.
	 * Loads the variable if it is a variable. Returns the number if it a number.
	 * Calls syntax error if token is invalid.
	 * 
	 * @param expression list of tokens to be computed
	 * @return temporary variable that holds result of expression or number if
	 *         expression is only one number
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private String computeExpression(List<String> expression)
			throws FileNotFoundException, UnsupportedEncodingException {
		// Expression is variable, number or invalid
		if (expression.size() == 1) {
			String token = expression.get(0);
			if (isNumber(token)) {
				return token;
			}
			// token is a variable
			else {
				checkVar(token);
				writer.println("%t" + ++tempNo + " = load i32* %" + token + "r");
				return "%t" + tempNo;
			}
		}
		// Operation, convert to post-fix
		else {
			try {
				// Make negative numbers one token
				for (int i = 0; i < expression.size(); i++) {
					if (expression.get(i).equals("-")
							&& (i == 0 || expression.get(i - 1).matches("\\+|\\-|\\*|\\/|\\(|\\,"))) {
						expression.remove(i);
						expression.set(i, "-" + expression.get(i));
					}
				}
				// Syntax error if there is no number or variable after opening parenthesis
				for (int i = 0; i < expression.size(); i++) {
					if (expression.get(i).equals("(")) {
						String firstToken = expression.get(i + 1);
						if (firstToken.equals(")")) {
							// System.out.println("Number or variable expected after (");
							syntaxError();
						}
					}
				}
				// Compute and replace choose function if it is detected
				for (int i = 0; i < expression.size(); i++) {
					if (expression.get(i).equals("choose")) {
						expression.remove(i);
						List<String> args = evaluateInsideParenthesis(expression.subList(i, expression.size()));
						String answer = choose(args);
						expression.add(i, answer);
						i = 0;
					}
				}
				// Turn to post-fix and do operations on popped elements
				expression = infixToPostfix(expression);
				Stack<String> operands = new Stack<String>();
				for (int i = 0; i < expression.size(); i++) {
					String c = expression.get(i);
					// Print sum code
					if (c.equals("+")) {
						fourOperations("add", operands);
					}
					// Print subtract code
					else if (c.equals("-")) {
						fourOperations("sub", operands);
					}
					// Print multiply code
					else if (c.equals("*")) {
						fourOperations("mul", operands);
					}
					// Print division code
					else if (c.equals("/")) {
						fourOperations("sdiv", operands);
					} else {
						operands.push(c);
					}
				}
				// stack is not empty, too many operands
				if (operands.size() != 1) {
					// System.out.println("stack is not empty, too many operands");
					syntaxError();
				}
				// Return if last token is temporary variable
				String answer = operands.pop();
				if (answer.charAt(0) == '%')
					return answer;
				// Evaluate last token if it is not temporary variable
				return computeExpression(new ArrayList<String>(Arrays.asList(answer)));
			} catch (Exception e) {
				// stack empty exception, too many operators
				// System.out.println("problem at evaluating expression");
				syntaxError();
				return "program won't come here";
			}
		}
	}

	/**
	 * Prints the command that carries out the given LLVM operation
	 * 
	 * @param operation add, sub, mul or sdiv
	 * @param operands  post-fix stack, apply operation on top two elements
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private void fourOperations(String operation, Stack<String> operands)
			throws FileNotFoundException, UnsupportedEncodingException {
		// Load operands if they are variables
		String rightSide = operands.pop();
		if (!isNumber(rightSide) && rightSide.charAt(0) != '%') {
			checkVar(rightSide);
			writer.println("%t" + ++tempNo + " = load i32* %" + rightSide + "r");
			rightSide = "%t" + tempNo;
		}
		String leftSide = operands.pop();
		if (!isNumber(leftSide) && leftSide.charAt(0) != '%') {
			checkVar(leftSide);
			writer.println("%t" + ++tempNo + " = load i32* %" + leftSide + "r");
			leftSide = "%t" + tempNo;
		}
		// Throw syntax error if there is zero division
		if (rightSide.equals("0") && operation.equals("sdiv")) {
			// System.out.println("Zero division");
			syntaxError();
		}
		// Write the command to do the operation
		writer.println("%t" + ++tempNo + " = " + operation + " i32 " + leftSide + ", " + rightSide);
		operands.push("%t" + tempNo);
	}

	/**
	 * Calls syntax error if number of opening and closing parenthesis don't match.
	 * 
	 * @param line Line of tokens that will be searched for parenthesis.
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private void checkParenthesis(ArrayList<String> line) throws FileNotFoundException, UnsupportedEncodingException {
		int parOpening = 0;
		for (String token : line) {
			if (token.equals("("))
				parOpening++;
			if (token.equals(")"))
				parOpening--;
		}
		if (parOpening != 0)
			syntaxError();
	}

	/**
	 * Takes tokens starting with '('. Looks for closing parenthesis. Removes
	 * parenthesis and expression inside it. Calls syntax error if first line is not
	 * '('.
	 * 
	 * @param tokens input to search expressions inside parenthesis
	 * @return expression inside the parenthesis
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private List<String> evaluateInsideParenthesis(List<String> tokens)
			throws FileNotFoundException, UnsupportedEncodingException {
		// Syntax error if there is no parenthesis after statement
		if (!tokens.get(0).equals("(")) {
			// System.out.println("paranthesis opening expected");
			syntaxError();
		}
		// remove ( from line
		tokens.remove(0);
		// take the expression inside the statement
		int openingParanthesis = 0;
		boolean finished = false;
		List<String> expression = new ArrayList<String>();
		while (!finished) {
			if (tokens.size() == 0) {
				// System.out.println("statement içi parantezler eşleşmiyor");
				syntaxError();
			} else if (tokens.get(0).equals(")")) {
				if (openingParanthesis == 0) {
					finished = true;
				} else {
					openingParanthesis--;
				}
			} else if (tokens.get(0).equals("(")) {
				openingParanthesis++;
			}
			expression.add(tokens.get(0));
			tokens.remove(0);
		}
		return expression;
	}

	/**
	 * Evaluates and removes expressions between curly brackets
	 * 
	 * @param lines all lines of input
	 * @param j     line number of starting curly bracket
	 * @return line number of last curly bracket
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private int evaluateInsideCurlyBrackets(ArrayList<ArrayList<String>> lines, int j)
			throws FileNotFoundException, UnsupportedEncodingException {
		try {
			ArrayList<String> tokens = lines.get(j);
			if (tokens.size() == 0) {
				// tokens = lines.get(++j);
				// lineNo++;
				// System.out.println("No curly bracket at statement line");
				syntaxError();
			}
			if (!tokens.get(0).equals("{")) {
				// System.out.println("statement den sonra { yok");
				syntaxError();
			}
			tokens.remove(0);
			ArrayList<ArrayList<String>> insideBrackets = new ArrayList<ArrayList<String>>();
			insideBrackets.add(new ArrayList<String>());
			String nextToken = "";
			int statementLine = j;
			while (!nextToken.equals("}")) {
				while (tokens.isEmpty()) {
					tokens = lines.get(++j); // EXCEPTION if no closing curly bracket
					insideBrackets.add(new ArrayList<String>());
					lineNo++;
				}
				nextToken = tokens.get(0);
				// Syntax error if nested curly brackets
				if (nextToken.equals("{")) {
					// System.out.println("nested curly bracket");
					syntaxError();
				}
				tokens.remove(0);
				insideBrackets.get(insideBrackets.size() - 1).add(nextToken);
			}
			insideBrackets.get(insideBrackets.size() - 1).remove("}");
			lineNo = statementLine;
			produceOutput(insideBrackets);
			lineNo -= 2;
		}
		// print syntax error if something goes wrong
		// If there is no curly bracket flow comes here with lineNo as last line number
		// If last line is empty lineNo is one less because of scanner behavior
		catch (Exception e) {
			// System.out.println("curly bracket işlerken hata");
			syntaxError();
		}
		return j - 1;
	}

	/**
	 * Turns infix expressions to post-fix expressions
	 * 
	 * @param expression list of tokens to be converted to post-fix
	 * @return post-fix expression
	 */
	private ArrayList<String> infixToPostfix(List<String> expression) {
		Stack<String> stack = new Stack<String>();
		ArrayList<String> postfix = new ArrayList<String>();
		for (String token : expression) {
			// find places for operators
			if (token.matches("\\+|\\-|\\*|\\/")) {
				while (!stack.isEmpty()) {
					String operatorAtTop = stack.pop();
					if (operatorAtTop.equals("(")) {
						stack.push(operatorAtTop);
						break;
					} else {
						// put the token with lowest precedence into the stack
						if ((operatorAtTop.equals("+") || operatorAtTop.equals("-"))
								&& (token.equals("*") || token.equals("/"))) {
							stack.push(operatorAtTop);
							break;
						} else {
							postfix.add(operatorAtTop);
						}
					}
				}
				stack.push(token);
			}
			// Directly push opening parenthesis into the stack
			else if (token.equals("(")) {
				stack.push(token);
			}
			// Pop to post-fix expression until matching parenthesis is found
			else if (token.equals(")")) {
				while (!stack.isEmpty()) {
					String match = stack.pop();
					if (match.equals("("))
						break;
					else {
						postfix.add(match);
					}
				}
			}
			// append number to post-fix
			else {
				postfix.add(token);
			}
		}
		// Pop all remaining items in the stack.
		while (!stack.isEmpty()) {
			postfix.add(stack.pop());
		}
		return postfix;
	}

	/**
	 * Prints the commands that implements choose function
	 * 
	 * @param args list of tokens in choose parenthesis
	 * @return the variable that holds the answer to the function
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private String choose(List<String> args) throws FileNotFoundException, UnsupportedEncodingException {
		try {
			String holdingVar = "%t" + ++tempNo;
			// Holds the arguments
			ArrayList<ArrayList<String>> expressions = new ArrayList<ArrayList<String>>(4);
			expressions.add(new ArrayList<String>());
			// Find 4 arguments and put them in expressions list
			for (int i = 0, j = 0; i < args.size(); i++) {
				String nextToken = args.get(i);
				// Ignore commas in choose functions in arguments
				if (nextToken.equals("choose")) {
					expressions.get(j).add(nextToken);
					int openingParenthesis = 0;
					boolean inChoose = true;
					while (inChoose) {
						i++;
						nextToken = args.get(i);
						if (nextToken.equals("(")) {
							openingParenthesis++;
						} else if (nextToken.equals(")")) {
							openingParenthesis--;
							if (openingParenthesis == 0)
								inChoose = false;
						}
						expressions.get(j).add(nextToken);
					}
				}
				// Commas separate arguments if not in another choose function
				if (nextToken.equals(",")) {
					j++;
					expressions.add(new ArrayList<String>());
					continue;
				}
				expressions.get(j).add(nextToken);
			}

			// Syntax error if there is no 4 arguments
			if (expressions.size() != 4) {
				// System.out.println("not 4 arguments in choose func");
				syntaxError();
			}

			// Compute each argument
			String expr1 = computeExpression(expressions.get(0));
			String expr2 = computeExpression(expressions.get(1));
			String expr3 = computeExpression(expressions.get(2));
			String expr4 = computeExpression(expressions.get(3));

			// Print LLVM commands for choose function with control flow statements

			// Allocates one extra variable, I could not find how to deallocate it
			writer.println(holdingVar + " = alloca i32");
			// If expr1 is 0
			writer.println("%t" + ++tempNo + " = icmp eq i32 " + expr1 + ", 0");
			writer.println("br i1 %t" + tempNo + ", label %choose0" + chooseNo + ", label %choose1" + chooseNo);
			// store expr2
			writer.println("choose0" + chooseNo + ":");
			writer.println("store i32 " + expr2 + ", i32* " + holdingVar);
			writer.println("br label %chooseend" + chooseNo);
			// Else come to this label
			writer.println("choose1" + chooseNo + ":");
			// If expr1 is positive
			writer.println("%t" + ++tempNo + " = icmp sgt i32 " + expr1 + ", 0");
			writer.println("br i1 %t" + tempNo + ", label %choose2" + chooseNo + ", label %choose3" + chooseNo);
			// Store expr3
			writer.println("choose2" + chooseNo + ":");
			writer.println("store i32 " + expr3 + ", i32* " + holdingVar);
			writer.println("br label %chooseend" + chooseNo);
			// Else store expr4
			writer.println("choose3" + chooseNo + ":");
			writer.println("store i32 " + expr4 + ", i32* " + holdingVar);
			writer.println("br label %chooseend" + chooseNo);
			writer.println("chooseend" + chooseNo++ + ":");
			// Load and return stored value
			writer.println("%t" + ++tempNo + " = load i32* " + holdingVar);
			return "%t" + tempNo;
		} catch (Exception e) {
			// System.out.println("problem inside choose");
			syntaxError();
			return "Program won't come here because syntax error ends it.";
		}
	}

	/**
	 * Checks if input is an acceptable variable name
	 * 
	 * @param varName variable name to be checked
	 * @return true if first character is alphabetic and others are alphanumeric
	 */
	private boolean acceptableVarName(String varName) {
		if (varName.matches("if|while|print|choose"))
			return false;
		if (isAlphabetic(varName.charAt(0))) {
			for (int i = 1; i < varName.length(); i++) {
				char c = varName.charAt(i);
				if (!isDigit(c) && !isAlphabetic(c))
					return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Checks if variable name is acceptable, allocates if it is not initialized
	 * 
	 * @param var variable name to be checked
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private void checkVar(String var) throws FileNotFoundException, UnsupportedEncodingException {
		if (!acceptableVarName(var)) {
			// System.out.println(var + " is not accaptable variable name");
			syntaxError();
		}
		var += "r";
		if (!vars.contains(var)) {
			vars.add(var);
			writer.println("%" + var + " = alloca i32");
			writer.println("store i32 0, i32* %" + var);
		}
	}

	/**
	 * Helps determining type of token. Calls syntax error if it is not an
	 * acceptable variable name
	 * 
	 * @param token string to be checked
	 * @return true if token is number, false if it is an acceptable variable name
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private boolean isNumber(String token) throws FileNotFoundException, UnsupportedEncodingException {
		// token is number or invalid
		if (isDigit(token.charAt(0)) || token.charAt(0) == '-') {
			try {
				Integer.parseInt(token);
			} catch (NumberFormatException nfe) {
				// System.out.println("token is invalid");
				syntaxError();
			}
			return true;
		}
		// token is a variable
		else {
			return false;
		}
	}

	/**
	 * Checks if input is between 0 and 9
	 * 
	 * @param c one character
	 * @return true if input is a digit
	 */
	private boolean isDigit(char c) {
		return (c >= 48 && c <= 57);
	}

	/**
	 * Checks if input is an English letter or underscore
	 * 
	 * @param c one character
	 * @return true if c is an English letter or underscore
	 */
	private boolean isAlphabetic(char c) {
		return (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || (c == 95);
	}

	/**
	 * This function is called when there is a syntax error. Clears the file and
	 * prints the LLVM code that prints syntax error.
	 * 
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private void syntaxError() throws FileNotFoundException, UnsupportedEncodingException {
		this.writer = new PrintWriter(outputPath, "UTF-8");
		printStartingLines();
		writer.println("call i32 (i8*, ...)* @printf(i8* getelementptr ([23 x i8]* @error.str, i32 0, i32 0), i32 "
				+ lineNo + ")");
		printEndingLines();
		System.exit(0);
	}
}
