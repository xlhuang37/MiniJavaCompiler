package miniJava.SyntacticAnalyzer;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	private Token _nextToken;
	
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
		this._nextToken = this._scanner.scan();
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}
	
	public Package parse() {
		// The first thing we need to parse is the Program symbol
		ClassDeclList classDeclarationList = new ClassDeclList();
		Package packageAST = new Package(classDeclarationList, new SourcePosition());
		try {
			parseProgram(classDeclarationList);
		}
		catch(SyntaxError e) {}
		return packageAST;
	}
	
	
	// Program ::= (ClassDeclaration)* eot
	private void parseProgram(ClassDeclList classDeclarationList) throws SyntaxError {
//      Uncomment debugLexer and Comment the following while loop to debug lexer.
//		debugLexer();
		
		while(this._currentToken.getTokenType() != TokenType.EOF) {
			ClassDecl classDeclaration = parseClassDeclaration();
			classDeclarationList.add(classDeclaration);
		}
//		
	}
	
	public void debugLexer() {
		while(this._currentToken.getTokenType() != TokenType.EOF) {
			System.out.println(this._currentToken.getTokenType() + " " + this._currentToken.getTokenText() + " " + (int) this._currentToken.getTokenText().charAt(0));
			this._currentToken = this._nextToken;
			this._nextToken = _scanner.scan();		
			}

	}
	
	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		// Not sure why classType is not used
		// ClassType type = new ClassType(new Identifier(this._currentToken), new SourcePosition());
		String className;
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		this.accept(TokenType.CLASS);
		className = this._currentToken.getTokenText();
		this.accept(TokenType.ID);
		this.accept(TokenType.LCurly);
		while(this._currentToken.getTokenType() != TokenType.RCurly) {
			boolean isPrivate = parseVisibility();
			boolean isStatic = parseAccess();
			ParameterDeclList pl = new ParameterDeclList();
			StatementList sl = new StatementList();
			String name;
			MemberDecl member;
			switch(this._currentToken.getTokenType()) {
				case TypeVoid:
					TypeDenoter voidType = new BaseType(TypeKind.VOID, new SourcePosition());
					accept(TokenType.TypeVoid);
					
					name = this._currentToken.getTokenText();
					accept(TokenType.ID);
					
					acceptMethod(pl, sl);
					
					member = new FieldDecl(isPrivate, isStatic, voidType, name, null);
					mdl.add(new MethodDecl(member, pl, sl, null));
					break;
				default:
					TypeDenoter generalType = parseType();
					name = this._currentToken.getTokenText();
					accept(TokenType.ID);
					member = new FieldDecl(isPrivate, isStatic, generalType, name, null);
					switch(this._currentToken.getTokenType()) {
						case LParen:
							acceptMethod(pl, sl);
							mdl.add(new MethodDecl(member, pl, sl, null));
							break;
						default:
							fdl.add(new FieldDecl(member, null));
							accept(TokenType.Semicolon);
							continue;
				}
			}
		}
		this.accept(TokenType.RCurly);
		
		
		ClassDecl classDeclaration = new ClassDecl(className, fdl, mdl, new SourcePosition());
		return classDeclaration;
	}
	
	private void acceptMethod(ParameterDeclList pl, StatementList sl){
		accept(TokenType.LParen);
		switch(this._currentToken.getTokenType()) {
			case RParen:
				break;
			default:
				parseParameterList(pl);
		}
		accept(TokenType.RParen);
		accept(TokenType.LCurly);
		while(this._currentToken.getTokenType() != TokenType.RCurly) {
			parseStatement(sl);
		}
		accept(TokenType.RCurly);
	}
	

	
	private void parseStatement(StatementList sl) {
		StatementList sl_r = new StatementList();
		Expression e;
		switch(this._currentToken.getTokenType()) {
			case LCurly:
				accept(TokenType.LCurly);
				while(this._currentToken.getTokenType() != TokenType.RCurly) {
					parseStatement(sl_r);
				}
				sl.add(new BlockStmt(sl_r, null));
				accept(TokenType.RCurly);
				break;
				
			case RETURN:
				accept(TokenType.RETURN);
				switch(this._currentToken.getTokenType()) {
					case Semicolon:
						sl.add(new ReturnStmt(null, null));
						break;
					default:
						e = parseExpression();
						sl.add(new ReturnStmt(e, null));
				}
				accept(TokenType.Semicolon);

				break;
				
			case IF:
				accept(TokenType.IF);
				accept(TokenType.LParen);
				Expression ifExpression = parseExpression();
				accept(TokenType.RParen);
				parseStatement(sl_r);
				Statement thenBlock = sl_r.get(0);
				if(this._currentToken.getTokenType() == TokenType.ELSE) {
						Statement elseBlock;
						accept(TokenType.ELSE);
						sl_r = new StatementList();
						parseStatement(sl_r);
						elseBlock = sl_r.get(0);
						sl.add(new IfStmt(ifExpression, thenBlock, elseBlock, null));
				}
				else {
					sl.add(new IfStmt(ifExpression, thenBlock, null));
				}
				
				break;
			case WHILE:
				accept(TokenType.WHILE);
				accept(TokenType.LParen);
				Expression whileCondition = parseExpression();
				accept(TokenType.RParen);
				parseStatement(sl_r);
				Statement whileBody = sl_r.get(0);
				sl.add(new WhileStmt(whileCondition, whileBody, null));
				break;
				
			case TypeInt:
				accept(TokenType.TypeInt);
				TypeDenoter intType = new BaseType(TypeKind.INT, null);
				if(_currentToken.getTokenType() == TokenType.LBracket) {
					accept(TokenType.LBracket);accept(TokenType.RBracket);
				    intType = new ArrayType(new BaseType(TypeKind.INT, null), null);
				}
				String intDeclName = this._currentToken.getTokenText();
				accept(TokenType.ID);
				accept(TokenType.Assign);
				Expression intDeclarationExpression = parseExpression();
				accept(TokenType.Semicolon);
				sl.add(new VarDeclStmt(new VarDecl(intType, intDeclName, null), intDeclarationExpression, null));
				break;
			case TypeBoolean:
				accept(TokenType.TypeBoolean);
				String booleanDeclName = this._currentToken.getTokenText();
				accept(TokenType.ID);
				accept(TokenType.Assign);
				Expression booleanDeclarationExpression = parseExpression();
				accept(TokenType.Semicolon);
				sl.add(new VarDeclStmt(new VarDecl(new BaseType(TypeKind.BOOLEAN, null), booleanDeclName, null), booleanDeclarationExpression, null));
				break;
				
			default:
				if(_currentToken.getTokenType() == TokenType.ID && _nextToken.getTokenType() == TokenType.LBracket) {
					Token assignmentToken = this._currentToken;
					accept(TokenType.ID);
					accept(TokenType.LBracket);
					if(_currentToken.getTokenType() == TokenType.RBracket) {
						accept(TokenType.RBracket);
						String variableName = this._currentToken.getTokenText();
						accept(TokenType.ID);
						accept(TokenType.Assign);
						Expression classArrayExpression = parseExpression();
						accept(TokenType.Semicolon);
						sl.add(new VarDeclStmt(
								new VarDecl(new ArrayType(new ClassType(new Identifier(assignmentToken), null), null) 
								, variableName, null)
								, classArrayExpression, null));
					}
					else {
						Expression indexExpr1 = parseExpression();
						accept(TokenType.RBracket);
						accept(TokenType.Assign);
						Expression indexExpr2 = parseExpression();
						accept(TokenType.Semicolon);
						sl.add(new IxAssignStmt(new IdRef(new Identifier(assignmentToken), null), indexExpr1, indexExpr2, null));
					}
				}
				else if (_currentToken.getTokenType() == TokenType.ID && _nextToken.getTokenType() == TokenType.ID) {
					Token classToken = this._currentToken;
					accept(TokenType.ID);
					String idName = this._currentToken.getTokenText();
					accept(TokenType.ID);
					accept(TokenType.Assign);
					Expression idExpression = parseExpression();
					accept(TokenType.Semicolon);
					sl.add(new VarDeclStmt(
							new VarDecl(new ClassType(new Identifier(classToken), null) 
							, idName, null)
							, idExpression, null));
				}
				else {
					Reference ref = parseReference();
					switch(this._currentToken.getTokenType()) {
						case Assign:
							accept(TokenType.Assign);
							Expression assignExpression = parseExpression();
							accept(TokenType.Semicolon);
							sl.add(new AssignStmt(ref, assignExpression, null));
							break;
						case LBracket:
							accept(TokenType.LBracket);
							Expression indexExpression = parseExpression();
							accept(TokenType.RBracket);
							accept(TokenType.Assign);
							Expression indexAssignValue = parseExpression();
							accept(TokenType.Semicolon);
							sl.add(new IxAssignStmt(ref, indexExpression, indexAssignValue, null));
							break;
						default:
							ExprList arguments  = new ExprList();
							
							accept(TokenType.LParen);
							if(this._currentToken.getTokenType() != TokenType.RParen) {
								parseArgumentList(arguments);
							}
							accept(TokenType.RParen);
							accept(TokenType.Semicolon);
							sl.add(new CallStmt(ref, arguments, null));
					}
				}
		}
	}
	
	private void parseArgumentList(ExprList arguments) {
		arguments.add(parseExpression());
		while(this._currentToken.getTokenType() == TokenType.Comma) {
			accept(TokenType.Comma);
			arguments.add(parseExpression());
		}
		
	}

	private Reference parseReference() {
		Reference endRef = null;
		QualRef currQualRef = null;
		switch(this._currentToken.getTokenType()){
			case ID:
				endRef = new IdRef(new Identifier(this._currentToken), null);
				accept(TokenType.ID);
				break;
			case THIS:
				endRef = new ThisRef(null);
				accept(TokenType.THIS);
				break;
			default:
				this._errors.reportError("error parsing reference");
		}
		
		int counter = 0;
		while(this._currentToken.getTokenType() == TokenType.Dot) {
			if(counter == 0) {
				currQualRef =  new QualRef(endRef,null, null);
			} else {
				QualRef nextQualRef = new QualRef(currQualRef, null, null);
				currQualRef = nextQualRef;
			}
			
			accept(TokenType.Dot);
			switch(this._currentToken.getTokenType()){
				case ID:
					currQualRef.id = new Identifier(this._currentToken);
					accept(TokenType.ID);

					break;
				default:
					this._errors.reportError("error parsing reference");
			}
			counter += 1;
		}
		
		if(currQualRef == null) {
			return endRef;
		}
		else {
			return currQualRef;
		}
		
		
	}
/* A is Expression
 * A = B (|| B)*
 * B = C (&& C)*
 * C = D (== D)* | D(!= D)*
 * D = 
 * 
 * 
 * */
	private Expression parseConjunction() {
		BinaryExpr binaryExpression = new BinaryExpr(null, null, null, null);
		Expression leftExpression = parseEquality();
		binaryExpression.left = leftExpression;
		int counter = 0;
		while(this._currentToken.getTokenText().equals("&&")) {
			Operator operator = new Operator(this._currentToken);
			acceptBinop();
			Expression rightExpression = parseEquality();
			if(binaryExpression.right==null) {
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			else {
				BinaryExpr nextBinaryExpression = new BinaryExpr(null, null, null, null);
				nextBinaryExpression.left = binaryExpression;
				binaryExpression = nextBinaryExpression;
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			counter += 1;
		}
		if(counter == 0) {return binaryExpression.left;}
		else {
			if(binaryExpression.right == null) {return binaryExpression.left;}
			else{return binaryExpression;}
		}
	}
	
	private Expression parseEquality() {
		BinaryExpr binaryExpression = new BinaryExpr(null, null, null, null);
		Expression leftExpression = parseRelational();
		binaryExpression.left = leftExpression;
		int counter = 0;
		while(this._currentToken.getTokenText().equals("!=")
				|| this._currentToken.getTokenText().equals("==")) {
			Operator operator = new Operator(this._currentToken);
			acceptBinop();
			Expression rightExpression = parseRelational();
			if(binaryExpression.right==null) {
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			else {
				BinaryExpr nextBinaryExpression = new BinaryExpr(null, null, null, null);
				nextBinaryExpression.left = binaryExpression;
				binaryExpression = nextBinaryExpression;
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			counter += 1;
		}
		if(counter == 0) {return binaryExpression.left;}
		else {
			if(binaryExpression.right == null) {return binaryExpression.left;}
			else{return binaryExpression;}
		}
	}
	private Expression parseRelational() {
		BinaryExpr binaryExpression = new BinaryExpr(null, null, null, null);
		Expression leftExpression = parseAdditive();
		binaryExpression.left = leftExpression;
		int counter = 0;
		while(this._currentToken.getTokenText().equals("<=")
				|| this._currentToken.getTokenText().equals(">=")
				|| this._currentToken.getTokenText().equals("<")
				|| this._currentToken.getTokenText().equals(">")
				) {
			Operator operator = new Operator(this._currentToken);
			acceptBinop();
			Expression rightExpression = parseAdditive();
			if(binaryExpression.right==null) {
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			else {
				BinaryExpr nextBinaryExpression = new BinaryExpr(null, null, null, null);
				nextBinaryExpression.left = binaryExpression;
				binaryExpression = nextBinaryExpression;
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			counter += 1;
		}
		if(counter == 0) {return binaryExpression.left;}
		else {
			if(binaryExpression.right == null) {return binaryExpression.left;}
			else{return binaryExpression;}
		}
	}
	private Expression parseAdditive() {
		BinaryExpr binaryExpression = new BinaryExpr(null, null, null, null);
		Expression leftExpression = parseMultiplicative();
		binaryExpression.left = leftExpression;
		int counter = 0;
		while(this._currentToken.getTokenText().equals("+")
				|| this._currentToken.getTokenText().equals("-")) {
			Operator operator = new Operator(this._currentToken);
			acceptBinop();
			Expression rightExpression = parseMultiplicative();
			if(binaryExpression.right==null) {
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			else {
				BinaryExpr nextBinaryExpression = new BinaryExpr(null, null, null, null);
				nextBinaryExpression.left = binaryExpression;
				binaryExpression = nextBinaryExpression;
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			counter += 1;
		}
		if(counter == 0) {return binaryExpression.left;}
		else {
			if(binaryExpression.right == null) {return binaryExpression.left;}
			else{return binaryExpression;}
		}
	}
	private Expression parseMultiplicative() {
		BinaryExpr binaryExpression = new BinaryExpr(null, null, null, null);
		Expression leftExpression = parseUnary();
		binaryExpression.left = leftExpression;
		int counter = 0;
		while(this._currentToken.getTokenText().equals("*")
				|| this._currentToken.getTokenText().equals("/")) {
			Operator operator = new Operator(this._currentToken);
			acceptBinop();
			Expression rightExpression = parseUnary();
			if(binaryExpression.right==null) {
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			else {
				BinaryExpr nextBinaryExpression = new BinaryExpr(null, null, null, null);
				nextBinaryExpression.left = binaryExpression;
				binaryExpression = nextBinaryExpression;
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			counter += 1;
		}
		if(counter == 0) {return binaryExpression.left;}
		else {
			if(binaryExpression.right == null) {return binaryExpression.left;}
			else{return binaryExpression;}
		}
	}
	private Expression parseUnary() {
		if(this._currentToken.getTokenType() == TokenType.Unop || this._currentToken.getTokenType() == TokenType.BinUnopMinus) {
			Operator unopOperator = new Operator(this._currentToken);
			acceptUnop();
			Expression unopExpression = parseUnary();
			return new UnaryExpr(unopOperator, unopExpression, null);
		}
		else {
			Expression currExpression = parseParenthesis();
			return currExpression;
		}
	}
	private Expression parseParenthesis() {
		if(this._currentToken.getTokenType() == TokenType.LParen) {
			accept(TokenType.LParen);
			Expression insideParenExpression = parseExpression();
			accept(TokenType.RParen);
			return insideParenExpression;
		}
		else {
			switch(this._currentToken.getTokenType()) {
			case IntLiteral:
				IntLiteral intLiteral = new IntLiteral(_currentToken);
				accept(TokenType.IntLiteral);
				return new LiteralExpr(intLiteral, null);
			case TrueLiteral:
				BooleanLiteral trueLiteral = new BooleanLiteral(_currentToken);
				accept(TokenType.TrueLiteral);
				return new LiteralExpr(trueLiteral, null);
			case FalseLiteral:
				BooleanLiteral falseLiteral = new BooleanLiteral(_currentToken);
				accept(TokenType.FalseLiteral);
				return new LiteralExpr(falseLiteral, null);
			case NEW:
				accept(TokenType.NEW);
				switch(this._currentToken.getTokenType()) {
					case ID:
						Identifier newObjectIdentifier = new Identifier(this._currentToken);
						accept(TokenType.ID);
						switch(this._currentToken.getTokenType()) {
							case LParen:
								accept(TokenType.LParen);
								accept(TokenType.RParen);
								return new NewObjectExpr(new ClassType(newObjectIdentifier, null), null);
							default:
								accept(TokenType.LBracket);
								Expression newArrayExpression = parseExpression();
								accept(TokenType.RBracket);
								return new NewArrayExpr(new ClassType(newObjectIdentifier, null), newArrayExpression, null);
						}
					default:
						accept(TokenType.TypeInt);
						accept(TokenType.LBracket);
						Expression newArrayExpression = parseExpression();
						accept(TokenType.RBracket);
						return new NewArrayExpr(new BaseType(TypeKind.INT, null), newArrayExpression, null);
				}
			case LParen:
				accept(TokenType.LParen);
				Expression expressionInParen = parseExpression();
				accept(TokenType.RParen);
				return expressionInParen;
			default:
				Expression referenceExpression = parseReferenceInExpression();
				return referenceExpression;
		}
		}
	}
	
	private Expression parseExpression() {
		
		BinaryExpr binaryExpression = new BinaryExpr(null, null, null, null);
		Expression leftExpression = parseConjunction();
		binaryExpression.left = leftExpression;
		int counter = 0;
		while(this._currentToken.getTokenText().equals("||")) {
			Operator operator = new Operator(this._currentToken);
			acceptBinop();
			Expression rightExpression = parseConjunction();
			if(binaryExpression.right==null) {
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			else {
				BinaryExpr nextBinaryExpression = new BinaryExpr(null, null, null, null);
				nextBinaryExpression.left = binaryExpression;
				binaryExpression = nextBinaryExpression;
				binaryExpression.operator = operator;
				binaryExpression.right = rightExpression;
			}
			counter += 1;
		}
		if(counter == 0) {return binaryExpression.left;}
		else {
			if(binaryExpression.right == null) {return binaryExpression.left;}
			else{return binaryExpression;}
		}
	}
	

	
	private Expression parseReferenceInExpression() {
		Reference newRef = parseReference();
		switch(this._currentToken.getTokenType()) {
			case LParen:
				ExprList argList = new ExprList();
				accept(TokenType.LParen);
				if(this._currentToken.getTokenType() != TokenType.RParen) {
					parseArgumentList(argList);
				}
				accept(TokenType.RParen);
				return new CallExpr(newRef, argList, null);
			case LBracket:
				accept(TokenType.LBracket);
				Expression indexExpression = parseExpression();
				accept(TokenType.RBracket);
				return new IxExpr(newRef, indexExpression, null);
			default:
				return new RefExpr(newRef, null);
		}
	}

	private void parseParameterList(ParameterDeclList pl) {
		TypeDenoter type = parseType();
		String name = this._currentToken.getTokenText();
		pl.add(new ParameterDecl(type, name, null));
		accept(TokenType.ID);
		while(this._currentToken.getTokenType() == TokenType.Comma) {
			accept(TokenType.Comma);	
			type = parseType();
			name = this._currentToken.getTokenText();
			accept(TokenType.ID);	
			pl.add(new ParameterDecl(type, name, null));
		}
		
	}
	
	private TypeDenoter parseType() {
		TypeDenoter type;
		if(this._nextToken.getTokenType() == TokenType.LBracket) {
			switch(this._currentToken.getTokenType()) {
				case TypeInt:
					type = new ArrayType(new BaseType(TypeKind.INT, null), null);
					accept(TokenType.TypeInt);
					break;
				default:
					type = new ArrayType(new ClassType(new Identifier(this._currentToken), null), null);
					accept(TokenType.ID);
				}
				accept(TokenType.LBracket);
				accept(TokenType.RBracket);
		}
		else {
			switch(this._currentToken.getTokenType()) {
			case TypeInt:
				type = new BaseType(TypeKind.INT, null);
				accept(TokenType.TypeInt);
				break;
			case TypeBoolean:
				type = new BaseType(TypeKind.BOOLEAN, null);
				accept(TokenType.TypeBoolean);
				break;
			default:
				type = new ClassType(new Identifier(this._currentToken), null);
				accept(TokenType.ID);
			}
		}
		return type;
	};
	
	
	private boolean parseAccess() {
		switch(this._currentToken.getTokenType()) {
			case AccessStatic:
				accept(TokenType.AccessStatic);
				return true;
			default:
				return false;
		}
	}
	
	private boolean parseVisibility() {
		switch(this._currentToken.getTokenType()) {
			case VisibilityPublic:
				accept(TokenType.VisibilityPublic);
				return false;
			case VisibilityPrivate:
				accept(TokenType.VisibilityPrivate);
				return true;
			default:
				return false;
	}
	}
	
	private boolean isBinop() {
		if(this._currentToken.getTokenType() == TokenType.BinUnopMinus) {
			return true;
		}
		else if (this._currentToken.getTokenType() == TokenType.Binop){
			return true;
		}
		else {
			return false;
		}
	}
	
	
//	private boolean isUnop() {
//		if(this._currentToken.getTokenType() == TokenType.BinUnopMinus) {
//			return true;
//		}
//		else if (this._currentToken.getTokenType() == TokenType.Unop){
//			return true;
//		}
//		else {
//			return false;
//		}
//	}
	
	private void acceptBinop(){
		if(this._currentToken.getTokenType() == TokenType.BinUnopMinus) {
			accept(TokenType.BinUnopMinus);
		}
		else{
			accept(TokenType.Binop);
		}
	}
	
	private void acceptUnop(){
		if(this._currentToken.getTokenType() == TokenType.BinUnopMinus) {
			accept(TokenType.BinUnopMinus);
		}
		else{
			accept(TokenType.Unop);
		}
	}
	
	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if( _currentToken.getTokenType() == expectedType) {
		//   For Debugging
//		    System.out.println(expectedType);
			_currentToken = _nextToken;
			_nextToken = this._scanner.scan();
			while(this._currentToken.getTokenType() == TokenType.Comment) {
//			    System.out.println(TokenType.Comment);
				_currentToken = _nextToken;
				_nextToken = this._scanner.scan();
			}
			return;
		}
		else if (this._currentToken.getTokenType() == TokenType.Comment) {
			//   For Debugging
			while(this._currentToken.getTokenType() == TokenType.Comment) {
//			    System.out.println(TokenType.Comment);
				_currentToken = _nextToken;
				_nextToken = this._scanner.scan();
			}
			accept(expectedType);
			return;
		}
		else {
			String error_message = "Expected token: " + expectedType + ", " + "but got: " + _currentToken.getTokenType();
			this._errors.reportError(error_message);
			throw new SyntaxError();		
		}

	}
}
