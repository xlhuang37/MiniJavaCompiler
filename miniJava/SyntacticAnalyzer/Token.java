package miniJava.SyntacticAnalyzer;

public class Token {
	private TokenType type;
	private String text;
	private SourcePosition sourcePosition = null;
	
	public Token(TokenType typeArg, String textArg) {
		this.type = typeArg;
		this.text = textArg;
	}
	
	public TokenType getTokenType() {
		return this.type;
	}
	
	public String getTokenText() {
		return this.text;
	}
	
	public SourcePosition getTokenPosition() {
		return this.sourcePosition;
	}
}
