package cop5556sp17;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Scanner {
	/**
	 * Kind enum
	 */

	public static enum Kind {
		IDENT(""), INT_LIT(""), KW_INTEGER("integer"), KW_BOOLEAN("boolean"), 
		KW_IMAGE("image"), KW_URL("url"), KW_FILE("file"), KW_FRAME("frame"), 
		KW_WHILE("while"), KW_IF("if"), KW_TRUE("true"), KW_FALSE("false"), 
		SEMI(";"), COMMA(","), LPAREN("("), RPAREN(")"), LBRACE("{"), 
		RBRACE("}"), ARROW("->"), BARARROW("|->"), OR("|"), AND("&"), 
		EQUAL("=="), NOTEQUAL("!="), LT("<"), GT(">"), LE("<="), GE(">="), 
		PLUS("+"), MINUS("-"), TIMES("*"), DIV("/"), MOD("%"), NOT("!"), 
		ASSIGN("<-"), OP_BLUR("blur"), OP_GRAY("gray"), OP_CONVOLVE("convolve"), 
		KW_SCREENHEIGHT("screenheight"), KW_SCREENWIDTH("screenwidth"), 
		OP_WIDTH("width"), OP_HEIGHT("height"), KW_XLOC("xloc"), KW_YLOC("yloc"), 
		KW_HIDE("hide"), KW_SHOW("show"), KW_MOVE("move"), OP_SLEEP("sleep"), 
		KW_SCALE("scale"), EOF("eof");

		Kind(String text) {
			this.text = text;
		}
		
		final String text;

		String getText() {
			return text;
		}
	}

	public static enum State {
		START,
		IN_IDENT,
		IN_DIGIT,
		IN_COMMENT,
		AFTER_OR,
		AFTER_EQ,
		AFTER_NOT,
		AFTER_LT,
		AFTER_GT,
		AFTER_MINUS,
		AFTER_BARMINUS,
		AFTER_DIV;
	}
	/**
	 * Thrown by Scanner when an illegal character is encountered
	 */
	@SuppressWarnings("serial")
	public static class IllegalCharException extends Exception {
		public IllegalCharException(String message) {
			super(message);
		}
	}

	/**
	 * Thrown by Scanner when an int literal is not a value that can be represented by an int.
	 */
	@SuppressWarnings("serial")
	public static class IllegalNumberException extends Exception {
		public IllegalNumberException(String message){
			super(message);
		}
	}


	/**
	 * Holds the line and position in the line of a token.
	 */
	static class LinePos {
		public final int line;
		public final int posInLine;

		public LinePos(int line, int posInLine) {
			super();
			this.line = line;
			this.posInLine = posInLine;
		}

		@Override
		public String toString() {
			return "LinePos [line=" + line + ", posInLine=" + posInLine + "]";
		}
	}


	public class Token {
		public final Kind kind;
		public final int pos;  //position in input array
		public final int length;
		
		//returns the text of this Token
		public String getText() {
			return chars.substring(pos, pos+length);
		}

		//returns a LinePos object representing the line and column of this Token
		LinePos getLinePos(){
			//System.out.println("pos : "+ this.pos);
			int lineNumber = Collections.binarySearch(lineStartingPos, this.pos);
			//System.out.println("computed : "+ lineNumber);
			if(lineNumber ==0)
			{
				return new LinePos(0,pos); 
			}
			
			if(lineNumber<0)
			{
				
					lineNumber = Math.abs(lineNumber);
					lineNumber-=2;	
			}
			return new LinePos(lineNumber,pos - lineStartingPos.get(lineNumber)-1);
			
		}

		Token(Kind kind, int pos, int length) {
			this.kind = kind;
			this.pos = pos;
			this.length = length;
		}

		/** 
		 * Precondition:  kind = Kind.INT_LIT,  the text can be represented with a Java int.
		 * Note that the validity of the input should have been checked when the Token was created.
		 * So the exception should never be thrown.
		 * 
		 * @return  int value of this token, which should represent an INT_LIT
		 * @throws NumberFormatException
		 */
		public int intVal() throws NumberFormatException{
			return Integer.parseInt(chars.substring(this.pos, this.pos+this.length));
		}
		
		public boolean isKind(Kind kind)
		{
			if(this.kind == kind)
				return true;
			return false;
		}
		
		@Override
		  public int hashCode() {
		   final int prime = 31;
		   int result = 1;
		   result = prime * result + getOuterType().hashCode();
		   result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		   result = prime * result + length;
		   result = prime * result + pos;
		   return result;
		  }

		  @Override
		  public boolean equals(Object obj) {
		   if (this == obj) {
		    return true;
		   }
		   if (obj == null) {
		    return false;
		   }
		   if (!(obj instanceof Token)) {
		    return false;
		   }
		   Token other = (Token) obj;
		   if (!getOuterType().equals(other.getOuterType())) {
		    return false;
		   }
		   if (kind != other.kind) {
		    return false;
		   }
		   if (length != other.length) {
		    return false;
		   }
		   if (pos != other.pos) {
		    return false;
		   }
		   return true;
		  }

		 

		  private Scanner getOuterType() {
		   return Scanner.this;
		  }
	}

	Scanner(String chars) {
		this.chars = chars;
		tokens = new ArrayList<Token>();
		lineStartingPos = new ArrayList<Integer>();
		lineStartingPos.add(0);
		kindHashMap = new HashMap<String, Kind>();
		for(Kind k : Kind.values())
		{
			kindHashMap.put(k.getText(), k);
		}
		
	}

	/**
	 * Initializes Scanner object by traversing chars and adding tokens to tokens list.
	 * 
	 * @return this scanner
	 * @throws IllegalCharException
	 * @throws IllegalNumberException
	 */
	public Scanner scan() throws IllegalCharException, IllegalNumberException {
		int pos = 0;
		int length = chars.length();
		State state = State.START;
		int startPos = 0;
		int ch;
		while (pos <= length) {
			ch = pos < length ? chars.charAt(pos) : -1;
			switch (state) {
			case START: 
			{
				pos = skipWhiteSpace(pos);
				ch = pos < length ? chars.charAt(pos) : -1;
				startPos = pos;
				switch (ch) {
				case -1: {tokens.add(new Token(Kind.EOF, pos, 0)); pos++;}  break;
				case '&': {tokens.add(new Token(Kind.AND, startPos, 1));pos++;} break;
				case '+': {tokens.add(new Token(Kind.PLUS, startPos, 1));pos++;} break;
				case '%': {tokens.add(new Token(Kind.MOD, startPos, 1));pos++;} break;
				case '*': {tokens.add(new Token(Kind.TIMES, startPos, 1));pos++;} break;
				case ';': {tokens.add(new Token(Kind.SEMI, startPos, 1));pos++;} break;
				case ',': {tokens.add(new Token(Kind.COMMA, startPos, 1));pos++;} break;
				case '(': {tokens.add(new Token(Kind.LPAREN, startPos, 1));pos++;} break;
				case ')': {tokens.add(new Token(Kind.RPAREN, startPos, 1));pos++;} break;
				case '{': {tokens.add(new Token(Kind.LBRACE, startPos, 1));pos++;} break;
				case '}': {tokens.add(new Token(Kind.RBRACE, startPos, 1));pos++;} break;
				case '0': {tokens.add(new Token(Kind.INT_LIT,startPos, 1));pos++;}break;
				case '=': {state = State.AFTER_EQ;pos++;}break;
				case '>': {state = State.AFTER_GT;pos++;}break;
				case '|': {state = State.AFTER_OR;pos++;}break;
				case '-': {state = State.AFTER_MINUS;pos++;}break;
				case '!': {state = State.AFTER_NOT;pos++;}break;
				case '<': {state = State.AFTER_LT;pos++;}break;
				case '/': {state = State.AFTER_DIV;pos++;}break;
				default: {
					if (Character.isDigit(ch)) {state = State.IN_DIGIT;pos++;} 
					else if (Character.isJavaIdentifierStart(ch)) {
						state = State.IN_IDENT;pos++;
					} 
					else {throw new IllegalCharException(
							"Illegal char '" +(char)ch+"' was found at pos - "+pos);
					}
				}
				} 
			}
			break;
			case AFTER_EQ: 
			{
				switch(ch) {
				case '=': {tokens.add(new Token(Kind.EQUAL, startPos, 2));pos++;state = State.START;} break;
				default: {
					throw new IllegalCharException(
							"Illegal char '" +(char)ch+"' was found at pos - "+pos);
				}
				}
			}
			break;
			case AFTER_GT: 
			{
				switch(ch) {
				case '=': {tokens.add(new Token(Kind.GE, startPos, 2));pos++;state = State.START;} break;
				default: {
					tokens.add(new Token(Kind.GT, startPos, 1));state = State.START;
				}
				}
			}
			break;
			case AFTER_NOT: 
			{
				switch(ch) {
				case '=': {tokens.add(new Token(Kind.NOTEQUAL, startPos, 2));pos++;state = State.START;} break;
				default: {
					tokens.add(new Token(Kind.NOT, startPos, 1));state = State.START;
				}
				}
			}
			break;
			case AFTER_MINUS: 
			{
				switch(ch) {
				case '>': {tokens.add(new Token(Kind.ARROW, startPos, 2));pos++;state = State.START;} break;
				default: {
					tokens.add(new Token(Kind.MINUS, startPos, 1));state = State.START;
				}
				}
			}
			break;
			case AFTER_OR: 
			{
				switch(ch) {
				case '-': {state = State.AFTER_BARMINUS;pos++;}break;
				default: {
					tokens.add(new Token(Kind.OR, startPos, 1));state = State.START;
				}
				}
			}
			break;
			case AFTER_BARMINUS: 
			{
				switch(ch) {
				case '>': {tokens.add(new Token(Kind.BARARROW, startPos, 3));pos++;state = State.START;} break;
				default: {
					tokens.add(new Token(Kind.OR, startPos, 1));
					tokens.add(new Token(Kind.MINUS, startPos+1, 1));
					state = State.START;
				}
				}
			}
			break;
			case AFTER_LT: 
			{
				switch(ch) {
				case '=': {tokens.add(new Token(Kind.LE, startPos, 2));pos++;state = State.START;} break;
				case '-': {tokens.add(new Token(Kind.ASSIGN, startPos, 2));pos++;state = State.START;} break;
				default: {
					tokens.add(new Token(Kind.LT, startPos, 1));
					state = State.START;
				}
				}
			}
			break;
			case AFTER_DIV: 
			{
				switch(ch) {
				case '*': {state = State.IN_COMMENT;pos++;}break;
				default: {
					tokens.add(new Token(Kind.DIV, startPos, 1));state = State.START;
				}
				}
			}
			break;
			case IN_DIGIT: 
			{
				
				if (Character.isDigit(ch)) {
					pos++;
				}
				else {
					String number = null;
					try {
						number = chars.substring(startPos, pos);
						Integer.parseInt(number);
					}
					catch(NumberFormatException ex)
					{
						throw new Scanner.IllegalNumberException(number + "too large to parse as integer.");
					}
					tokens.add(new Token(Kind.INT_LIT, startPos, pos-startPos));state = State.START;
				}
			}
			break;
			case IN_IDENT: 
			{

				if (Character.isJavaIdentifierPart(ch)) {
					pos++;
				}
				else {
					String word = chars.substring(startPos, pos);
					Kind token = null;

					token = kindHashMap.get(word);

					if(token!=null)
					{
						tokens.add(new Token(token, startPos, pos-startPos));state = State.START;
					}
					else
					{
						tokens.add(new Token(Kind.IDENT, startPos, pos-startPos));state = State.START;
					}
				}
			}
			break;
			case IN_COMMENT:
			{
				
				if(pos<length-1)
				{
					isNextLine(pos);
					if(chars.charAt(pos)=='*' && chars.charAt(pos+1) == '/')
					{
						state = State.START;
						pos++;
					}
				}
				pos++;
			}
			break;
			default:  assert false;
			}// switch(state)
		}
		tokens.add(new Token(Kind.EOF,pos,0));
		return this;  
	}



	private int skipWhiteSpace(int pos) {
		int length = chars.length();
		while(pos<length && Character.isWhitespace(chars.charAt(pos)))
		{
			isNextLine(pos);
			pos++;
		}
		return pos;
	}


	private boolean isNextLine(int pos) {
		if((chars.charAt(pos) == '\n'))
				{
			
					lineStartingPos.add(pos);
					return true;
				}
		return false;
	}

	final ArrayList<Token> tokens;
	final ArrayList<Integer> lineStartingPos;
	final HashMap<String, Kind> kindHashMap;
	final String chars;
	int tokenNum;

	/*
	 * Return the next token in the token list and update the state so that
	 * the next call will return the Token..  
	 */
	public Token nextToken() {
		if (tokenNum >= tokens.size())
			return null;
		return tokens.get(tokenNum++);
	}

	 /*
	 * Return the next token in the token list without updating the state.
	 * (So the following call to next will return the same token.)
	 */public Token peek() {
	    if (tokenNum >= tokens.size())
	        return null;
	    return tokens.get(tokenNum);
	}



	/**
	 * Returns a LinePos object containing the line and position in line of the 
	 * given token.  
	 * 
	 * Line numbers start counting at 0
	 * 
	 * @param t
	 * @return
	 */
	public LinePos getLinePos(Token t) {
		return t.getLinePos();
	}


}
