package miniJava;

import java.io.FileInputStream;
import java.io.IOException;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.SyntacticAnalyzer.*;
public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) throws IOException {
		// TODO: Instantiate the ErrorReporter object
		ErrorReporter errors = new ErrorReporter();
		// TODO: Check to make sure a file path is given in args
		if(args.length == 0) {
			throw new IOException("File Path Not Spcified");
		}
		// TODO: Create the inputStream using new FileInputStream
        FileInputStream in = new FileInputStream(args[0]);
		// TODO: Instantiate the scanner with the input stream and error object
        Scanner scanner = new Scanner(in);
		// TODO: Instantiate the parser with the scanner and error object
		Parser parser = new Parser(scanner, errors);
		// TODO: Call the parser's parse function
		AST ast = parser.parse();
		if(errors.hasErrors()) {
			System.out.println("Error");
			errors.outputErrors();
		}
		else {
//			System.out.println("Success");
		}
		ASTDisplay astDisplay = new ASTDisplay();
		astDisplay.showTree(ast);
		
		// TODO: Check if any errors exist, if so, println("Error")
		//  then output the errors

	}
}
