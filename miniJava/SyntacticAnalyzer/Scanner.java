package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

public class Scanner {
	private InputStream _in;
	private StringBuilder _currentText;
	private char _currentChar;
	
	public Scanner( InputStream in) {
		this._in = in;
		this._currentText = new StringBuilder();
		nextChar();
	}
	
	public Token scan() {
		// EOF
		
		TokenType toktype;
		Token token;
		// Skipping Whitespace when nothing in String.
		while(((int) this._currentChar > -1 && (int) this._currentChar < 33) && this._currentText.length() == 0) {
			skipIt();
		}
		// EOF
		if(this._currentChar == (char) -1) {
			toktype = TokenType.EOF;
			token = makeToken(toktype);
			return token;
		}

		// NumLiteral
		if(isNum(this._currentChar)) {
			while(isNum(this._currentChar)) {
				takeIt();
			}
			toktype = TokenType.IntLiteral;
			token = makeToken(toktype);
			return token;
		}
		// Identifier and Literals made of Alphabet and Number
		if(isAlphabet(this._currentChar)) {
			while(isAlphaNumeric(this._currentChar) || isNum(this._currentChar)) {
				takeIt();
			}
			switch(this._currentText.toString()) {
				case "new":
					toktype = TokenType.NEW;
					break;
				case "true":
					toktype = TokenType.TrueLiteral;
					break;
				case "false":
					toktype = TokenType.FalseLiteral;
					break;
				case "if":
					toktype = TokenType.IF;
					break;
				case "else":
					toktype = TokenType.ELSE;
					break;
				case "while":
					toktype = TokenType.WHILE;
					break;
				case "return":
					toktype = TokenType.RETURN;
					break;
				case "int":
					toktype = TokenType.TypeInt;
					break;
				case "boolean":
					toktype = TokenType.TypeBoolean;
					break;
				case "void":
					toktype = TokenType.TypeVoid;
					break;
				case "static":
					toktype = TokenType.AccessStatic;
					break;
				case "this":
					toktype = TokenType.THIS;
					break;
				case "public":
					toktype = TokenType.VisibilityPublic;
					break;
				case "private":
					toktype = TokenType.VisibilityPrivate;
					break;
				case "class":
					toktype = TokenType.CLASS;
					break;
				default:
					toktype = TokenType.ID;
			}
			token = makeToken(toktype);
			return token;
		}
		

		// All Remaining Cases
		switch(this._currentChar) {
			case '+':
				takeIt();
				toktype = TokenType.Binop;
				break;
			case '-':
				takeIt();
				toktype = TokenType.BinUnopMinus;
				break;
			case '*':
				takeIt();
				toktype = TokenType.Binop;
				break;
			case '/':
				takeIt();
				if(this._currentChar == '*') {
					takeIt();
					char prev_char = 0;
					while(this._currentChar != '/' || prev_char != '*') {
						if(this._currentChar == (char) -1) {toktype = TokenType.UNDEFINED; token = makeToken(toktype); return token;}
						prev_char = this._currentChar;
						takeIt();
					}
					takeIt();
					toktype = TokenType.Comment;
				}
				else if(this._currentChar == '/') {
					while(this._currentChar != '\n' 
					   && this._currentChar != '\r'
					   && this._currentChar != (char) -1) {
						takeIt();
					}
					if(this._currentChar == (char) -1) {toktype = TokenType.Comment; token = makeToken(toktype); return token;}
					skipIt();
					toktype = TokenType.Comment;
				}
				else {
					toktype = TokenType.Binop;
				}		
				break;
			case '>':
				takeIt();
				if(this._currentChar == '=') {
					takeIt();
					toktype = TokenType.Binop;
				}
				else {
					toktype = TokenType.Binop;
				}
				break;
			case '<':
				takeIt();
				if(this._currentChar == '=') {
					takeIt();
					toktype = TokenType.Binop;
				}
				else {
					toktype = TokenType.Binop;
				}
				break;
			case '=':
				takeIt();
				if(this._currentChar == '=') {
					takeIt();
					toktype = TokenType.Binop;
				}
				else {
					toktype = TokenType.Assign;
				}
				break;
			case '!':
				takeIt();
				if(this._currentChar == '=') {
					takeIt();
					toktype = TokenType.Binop;
				}
				else {
					toktype = TokenType.Unop;
				}
				break;
			case '&':
				takeIt();
				if(this._currentChar == '&') {
					takeIt();
					toktype = TokenType.Binop;
				}
				else {
					toktype = TokenType.UNDEFINED;
				}
				break;
			case '|':
				takeIt();
				if(this._currentChar == '|') {
					takeIt();
					toktype = TokenType.Binop;
				}
				else {
					toktype = TokenType.UNDEFINED;
				}
				break;
			// Brackets
			case '{':
				takeIt();
				toktype = TokenType.LCurly;
				break;
			case '}':
				takeIt();
				toktype = TokenType.RCurly;
				break;
			case '(':
				takeIt();
				toktype = TokenType.LParen;
				break;
			case ')':
				takeIt();
				toktype = TokenType.RParen;
				break;
			case '[':
				takeIt();
				toktype = TokenType.LBracket;
				break;
			case ']':
				takeIt();
				toktype = TokenType.RBracket;
				break;
			case ':':
				takeIt();
				toktype = TokenType.Colon;
				break;
			case ';':
				takeIt();
				toktype = TokenType.Semicolon;
				break;
			case ',':
				takeIt();
				toktype = TokenType.Comma;
				break;
			case '.':
				takeIt();
				toktype = TokenType.Dot;
				break;
			default:
				takeIt();
				toktype = TokenType.UNDEFINED;
		} 
		token = makeToken(toktype);
		return token; 
	}
	
	private boolean isAlphabet(int c) {
		if((c > 64 && c < 91) || (c > 96 && c <123)) {
			return true;
		}
		return false;
	}
	
	private boolean isAlphaNumeric(int c) {
		if((c > 64 && c < 91) || (c > 96 && c <123) || c == 95) {
			return true;
		}
		return false;
	}
	
	private boolean isNum(int c) {
		if( this._currentChar > 47 && this._currentChar < 58) {
			return true;
		}
		return false;
	}
	
	private void takeIt() {
		_currentText.append(_currentChar);
		nextChar();
	}
	
	private void skipIt() {
		nextChar();
	}
	
	private void nextChar() {
		try {
			int c = _in.read();
			_currentChar = (char)c;
			
			if(c==(-1)){
				this._currentChar = (char) -1;
			}
			
			if(c < -1 || c > 127) {throw new IOException("Non-ASCII Char Detected;");}
			
		} catch( IOException e ) {
			return;
		}
	}
	
	private Token makeToken( TokenType toktype ) {
		// TODO: return a new Token with the appropriate type and text
		//  contained in 
		String text = this._currentText.toString();
		this._currentText.setLength(0);
		Token token = new Token(toktype, text);
		return token;
	}
}
