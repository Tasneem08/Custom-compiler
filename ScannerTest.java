package cop5556sp17;

import static cop5556sp17.Scanner.Kind.AND;
import static cop5556sp17.Scanner.Kind.ARROW;
import static cop5556sp17.Scanner.Kind.BARARROW;
import static cop5556sp17.Scanner.Kind.COMMA;
import static cop5556sp17.Scanner.Kind.DIV;
import static cop5556sp17.Scanner.Kind.EQUAL;
import static cop5556sp17.Scanner.Kind.GE;
import static cop5556sp17.Scanner.Kind.GT;
import static cop5556sp17.Scanner.Kind.IDENT;
import static cop5556sp17.Scanner.Kind.INT_LIT;
import static cop5556sp17.Scanner.Kind.KW_BOOLEAN;
import static cop5556sp17.Scanner.Kind.KW_FALSE;
import static cop5556sp17.Scanner.Kind.KW_FILE;
import static cop5556sp17.Scanner.Kind.KW_FRAME;
import static cop5556sp17.Scanner.Kind.KW_HIDE;
import static cop5556sp17.Scanner.Kind.KW_IF;
import static cop5556sp17.Scanner.Kind.KW_IMAGE;
import static cop5556sp17.Scanner.Kind.KW_INTEGER;
import static cop5556sp17.Scanner.Kind.KW_MOVE;
import static cop5556sp17.Scanner.Kind.KW_SHOW;
import static cop5556sp17.Scanner.Kind.KW_TRUE;
import static cop5556sp17.Scanner.Kind.KW_WHILE;
import static cop5556sp17.Scanner.Kind.LBRACE;
import static cop5556sp17.Scanner.Kind.LE;
import static cop5556sp17.Scanner.Kind.LPAREN;
import static cop5556sp17.Scanner.Kind.LT;
import static cop5556sp17.Scanner.Kind.MINUS;
import static cop5556sp17.Scanner.Kind.MOD;
import static cop5556sp17.Scanner.Kind.NOT;
import static cop5556sp17.Scanner.Kind.NOTEQUAL;
import static cop5556sp17.Scanner.Kind.OR;
import static cop5556sp17.Scanner.Kind.PLUS;
import static cop5556sp17.Scanner.Kind.RBRACE;
import static cop5556sp17.Scanner.Kind.RPAREN;
import static cop5556sp17.Scanner.Kind.SEMI;
import static cop5556sp17.Scanner.Kind.TIMES;
import static cop5556sp17.Scanner.Kind.ASSIGN;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


import cop5556sp17.Scanner.IllegalCharException;
import cop5556sp17.Scanner.IllegalNumberException;
import cop5556sp17.Scanner.LinePos;
import cop5556sp17.Scanner.Token;
import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;

public class ScannerTest {

	@Rule
    public ExpectedException thrown = ExpectedException.none();
    
	Token checkNext(Scanner scanner, Scanner.Kind kind, int pos, String text) {
		Scanner.Token token = scanner.nextToken();
		assertEquals(kind, token.kind);
		assertEquals(pos, token.pos);
		assertEquals(text.length(), token.length);
		assertEquals(text, token.getText());
		return token;
	}

	// Don't use this with idents or numlits
	Token checkNext(Scanner scanner, Scanner.Kind kind, int pos) {
		Scanner.Token token = scanner.nextToken();
		assertEquals(kind, token.kind);
		assertEquals(pos, token.pos);
		String text = kind.getText();
		assertEquals(text.length(), token.length);
		assertEquals(text, token.getText());
		return token;
	}
	
	Token checkNext(Scanner scanner, Scanner.Kind kind) {
		Scanner.Token token = scanner.nextToken();
		assertEquals(kind, token.kind);
		return token;
	}

	Token getAndCheckEnd(Scanner scanner) {
		Scanner.Token token = scanner.nextToken();
		assertEquals(Scanner.Kind.EOF,token.kind);
		return token;
	}
	
	void checkPos(Scanner scanner, Token t, int line, int posInLine){
		LinePos p = scanner.getLinePos(t);
		assertEquals(line,p.line);
		assertEquals(posInLine, p.posInLine);
	}
	
	@Test
	public void testEmpty() throws IllegalCharException, IllegalNumberException {
		String input = "";
		Scanner scanner = new Scanner(input);
		scanner.scan();
	}

	@Test
	public void testSemiConcat() throws IllegalCharException, IllegalNumberException {
		//input string
		String input = ";;;";
		//create and initialize the scanner
		Scanner scanner = new Scanner(input);
		scanner.scan();
		//get the first token and check its kind, position, and contents
		Scanner.Token token = scanner.nextToken();
		assertEquals(SEMI, token.kind);
		assertEquals(0, token.pos);
		String text = SEMI.getText();
		assertEquals(text.length(), token.length);
		assertEquals(text, token.getText());
		//get the next token and check its kind, position, and contents
		Scanner.Token token1 = scanner.nextToken();
		assertEquals(SEMI, token1.kind);
		assertEquals(1, token1.pos);
		assertEquals(text.length(), token1.length);
		assertEquals(text, token1.getText());
		Scanner.Token token2 = scanner.nextToken();
		assertEquals(SEMI, token2.kind);
		assertEquals(2, token2.pos);
		assertEquals(text.length(), token2.length);
		assertEquals(text, token2.getText());
		//check that the scanner has inserted an EOF token at the end
		Scanner.Token token3 = scanner.nextToken();
		assertEquals(Scanner.Kind.EOF,token3.kind);
	}
	
	
	/**
	 * This test illustrates how to check that the Scanner detects errors properly. 
	 * In this test, the input contains an int literal with a value that exceeds the range of an int.
	 * The scanner should detect this and throw and IllegalNumberException.
	 * 
	 * @throws IllegalCharException
	 * @throws IllegalNumberException
	 */
	@Test
	public void testIntOverflowError() throws IllegalCharException, IllegalNumberException{
		String input = "9999999999999999";
		Scanner scanner = new Scanner(input);
		thrown.expect(IllegalNumberException.class);
		scanner.scan();		
	}

	@Test
	public void paranthesesCheck() throws IllegalCharException, IllegalNumberException {
		String input = ";()(;";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, SEMI, 0);
		checkNext(scanner, LPAREN, 1);
		checkNext(scanner, RPAREN, 2);
		checkNext(scanner, LPAREN, 3);
		checkNext(scanner, SEMI, 4);
		getAndCheckEnd(scanner);
	}

	@Test
	public void bracesCheck() throws IllegalCharException, IllegalNumberException {
		String input = "}{+)!(";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, RBRACE, 0);
		checkNext(scanner, LBRACE, 1);
		checkNext(scanner, PLUS, 2);
		checkNext(scanner, RPAREN, 3);
		checkNext(scanner, NOT, 4);
		checkNext(scanner, LPAREN, 5);
		getAndCheckEnd(scanner);
	}

	@Test
	public void notEqualCheck() throws IllegalCharException, IllegalNumberException {
		String input = "!!!=!=!,";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, NOT, 0);
		checkNext(scanner, NOT, 1);
		checkNext(scanner, NOTEQUAL, 2);
		checkNext(scanner, NOTEQUAL, 4);
		checkNext(scanner, NOT, 6);
		checkNext(scanner, COMMA, 7);
		getAndCheckEnd(scanner);
	}

	@Test
	public void arrowCheck() throws IllegalCharException, IllegalNumberException {
		String input = "--->->-";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, MINUS, 0);
		checkNext(scanner, MINUS, 1);
		checkNext(scanner, ARROW, 2);
		checkNext(scanner, ARROW, 4);
		checkNext(scanner, MINUS, 6);
		getAndCheckEnd(scanner);
	}

	@Test
	public void barArrowCheck() throws IllegalCharException, IllegalNumberException {
		String input = "|;|--->->-|->";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, OR, 0);
		checkNext(scanner, SEMI, 1);
		checkNext(scanner, OR, 2);
		checkNext(scanner, MINUS, 3);
		checkNext(scanner, MINUS, 4);
		checkNext(scanner, ARROW, 5);
		checkNext(scanner, ARROW, 7);
		checkNext(scanner, MINUS, 9);
		checkNext(scanner, BARARROW, 10);
		getAndCheckEnd(scanner);
	}

	@Test
	public void lessThanGreaterThanCheck() throws IllegalCharException, IllegalNumberException {
		String input = "<<<=>>>=>< ->-->";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, LT, 0);
		checkNext(scanner, LT, 1);
		checkNext(scanner, LE, 2);
		checkNext(scanner, GT, 4);
		checkNext(scanner, GT, 5);
		checkNext(scanner, GE, 6);
		checkNext(scanner, GT, 8);
		checkNext(scanner, LT, 9);
		checkNext(scanner, ARROW, 11);
		checkNext(scanner, MINUS, 13);
		checkNext(scanner, ARROW, 14);
		getAndCheckEnd(scanner);
	}
	
	@Test
	public void orMinusCheck() throws IllegalCharException, IllegalNumberException {
		String input = "|-> |-|-";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, BARARROW, 0);
		checkNext(scanner, OR, 4);
		checkNext(scanner, MINUS, 5);
		checkNext(scanner, OR, 6);
		checkNext(scanner, MINUS, 7);
		getAndCheckEnd(scanner);
	}

	@Test
	public void intLiteralsCheck() throws IllegalCharException, IllegalNumberException {
		String input = "123()+4+54321";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, INT_LIT, 0, "123");
		checkNext(scanner, LPAREN, 3);
		checkNext(scanner, RPAREN, 4);
		checkNext(scanner, PLUS, 5);
		checkNext(scanner, INT_LIT, 6, "4");
		checkNext(scanner, PLUS, 7);
		checkNext(scanner, INT_LIT, 8, "54321");
		getAndCheckEnd(scanner);
	}

	@Test
	public void test02() throws IllegalCharException, IllegalNumberException {
		String input = "a+b;a23a4";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, IDENT, 0, "a");
		checkNext(scanner, PLUS, 1);
		checkNext(scanner, IDENT, 2, "b");
		checkNext(scanner, SEMI, 3);
		checkNext(scanner, IDENT, 4, "a23a4");
		getAndCheckEnd(scanner);
	}

	@Test
	public void test03() throws IllegalCharException, IllegalNumberException {
		String input = "xyz_45 <- (56,a34)";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, IDENT, 0, "xyz_45");
		checkNext(scanner, ASSIGN, 7);
		checkNext(scanner, LPAREN, 10);
		checkNext(scanner, INT_LIT, 11, "56");
		checkNext(scanner, COMMA, 13);
		checkNext(scanner, IDENT, 14, "a34");
		checkNext(scanner, RPAREN, 17);
		getAndCheckEnd(scanner);
	}
	
	@Test
	public void keywordsCheck() throws IllegalCharException, IllegalNumberException {
		String input = "ifwhile;if;while;boolean;boolean0;integer;integer32|->frame->-image";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, IDENT, 0, "ifwhile");
		checkNext(scanner, SEMI, 7);
		checkNext(scanner, KW_IF, 8);
		checkNext(scanner, SEMI, 10);
		checkNext(scanner, KW_WHILE, 11);
		checkNext(scanner, SEMI, 16);
		checkNext(scanner, KW_BOOLEAN, 17);
		checkNext(scanner, SEMI, 24);
		checkNext(scanner, IDENT, 25, "boolean0");
		checkNext(scanner, SEMI, 33);
		checkNext(scanner, KW_INTEGER, 34);
		checkNext(scanner, SEMI, 41);
		checkNext(scanner, IDENT, 42, "integer32");
		checkNext(scanner, BARARROW, 51, "|->");
		checkNext(scanner, KW_FRAME, 54);
		checkNext(scanner, ARROW, 59);
		checkNext(scanner, MINUS, 61);
		checkNext(scanner, KW_IMAGE, 62);
		getAndCheckEnd(scanner);
	}

	@Test
	public void identifierAndLiteralCheck() throws IllegalCharException, IllegalNumberException {
		String input = "abc 234 a23";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, IDENT, 0, "abc");
		checkNext(scanner, INT_LIT, 4, "234");
		checkNext(scanner, IDENT, 8, "a23");
		getAndCheckEnd(scanner);
	}
	
	@Test
	public void trueAndFalseKeywordCheck() throws IllegalCharException, IllegalNumberException {
		String input = "true True false False _true$false";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, KW_TRUE, 0);
		checkNext(scanner, IDENT, 5, "True");
		checkNext(scanner, KW_FALSE, 10);
		checkNext(scanner, IDENT, 16, "False");
		checkNext(scanner, IDENT, 22, "_true$false");
		getAndCheckEnd(scanner);
	}

	@Test
	public void notCheck() throws IllegalCharException, IllegalNumberException {
		String input = "abc! !d";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, IDENT, 0, "abc");
		checkNext(scanner, NOT, 3);
		checkNext(scanner, NOT, 5);
		checkNext(scanner, IDENT, 6, "d");
		getAndCheckEnd(scanner);
	}

	@Test
	public void whitespacesTest() throws IllegalCharException, IllegalNumberException {
		String input = "   ;";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, SEMI, 3);
		getAndCheckEnd(scanner);
	}

	@Test
	public void escapSequencesTest() throws IllegalCharException, IllegalNumberException {
		String input = "\n\n \t \r;";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, SEMI, 6);
		getAndCheckEnd(scanner);
	}

	@Test
	public void linechangeCheck() throws IllegalCharException, IllegalNumberException {
		String input = "a\n\n\n  bc! !\n_d";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		Scanner.Token t = scanner.nextToken();
		assertEquals("a",t.getText());
		checkPos(scanner,t,0,0);
		t = scanner.nextToken();
		assertEquals("bc",t.getText());
		checkPos(scanner,t,3,2);
		t = scanner.nextToken();
		assertEquals("!",t.getText());
		checkPos(scanner,t,3,4);
		t = scanner.nextToken();
		assertEquals("!",t.getText());
		checkPos(scanner,t,3,6);
		t = scanner.nextToken();
		assertEquals("_d",t.getText());
		checkPos(scanner,t,4,0);		

	}
	
	@Test
	public void test01() throws IllegalCharException, IllegalNumberException {
		String input = "/*...a/**\nbc!/ /*/ /**/ !\nd/*.**/";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		Scanner.Token t = scanner.nextToken();
		checkPos(scanner,t,1,14);
		t = scanner.nextToken();
		checkPos(scanner,t,2,0);
		getAndCheckEnd(scanner);			
	}

	
	@Test
	public void invalidChar() throws IllegalCharException, IllegalNumberException{
		String input = "abc def/n345 #abc";
		Scanner scanner = new Scanner(input);
		thrown.expect(IllegalCharException.class);
		thrown.expectMessage("#");
		scanner.scan();
	}
	
	@Test
	public void literalSpecialChar() throws IllegalCharException, IllegalNumberException{
		String input = "$integer1 _variable02";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, IDENT, 0, "$integer1");
		checkNext(scanner, IDENT, 10, "_variable02");
	}
	
	@Test
	public void invalidCharLiteralCheck() throws IllegalCharException, IllegalNumberException{
		String input = "abc def/n345 abc#";
		Scanner scanner = new Scanner(input);
		thrown.expect(IllegalCharException.class);
		thrown.expectMessage("#");
		scanner.scan();
	}
	
	@Test
	public void closedComments() throws IllegalCharException, IllegalNumberException{
		String input = "/*This is a \n closed comment sec\rtion. */ xyz123";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, IDENT,42,"xyz123");
		getAndCheckEnd(scanner);			
	}
	
	@Test
	public void onlyIntegerLiterals() throws IllegalCharException, IllegalNumberException{
		String input="123 456";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		Scanner.Token t = scanner.nextToken();
		assertEquals(123,t.intVal());
		t = scanner.nextToken();
		assertEquals(456,t.intVal());	
		getAndCheckEnd(scanner);		
	}
	
	@Test
	public void singleOperators() throws IllegalCharException, IllegalNumberException {
		String input="*/%&&+";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, TIMES, 0);
		checkNext(scanner, DIV, 1);
		checkNext(scanner, MOD, 2);
		checkNext(scanner, AND, 3);
		checkNext(scanner, AND, 4);
		checkNext(scanner, PLUS, 5);
	}
	
	@Test
	public void emptyString() throws IllegalCharException, IllegalNumberException{
		String input="";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		getAndCheckEnd(scanner);		
	}
	
	@Test
	public void openComment() throws IllegalCharException, IllegalNumberException{
		String input="/**This comment block is not closed \n \r   ";
		Scanner scanner = new Scanner(input);
		scanner.scan();	
		getAndCheckEnd(scanner);	
	}
	
	@Test
	public void nestedComments() throws IllegalCharException, IllegalNumberException{
		String input="/**/*/";
		Scanner scanner = new Scanner(input);
		scanner.scan();	
		checkNext(scanner, TIMES, 4);
		checkNext(scanner, DIV, 5);
	}
	
	@Test
	public void zeroLiteralCheck() throws IllegalCharException, IllegalNumberException{
		String input="011";
		Scanner scanner = new Scanner(input);
		scanner.scan();	
		checkNext(scanner, INT_LIT, 0, "0");
		checkNext(scanner, INT_LIT, 1, "11");
	}
	
	@Test
	public void equalsOkay() throws IllegalCharException, IllegalNumberException{
		String input="== ==";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		checkNext(scanner, EQUAL, 0);
		checkNext(scanner, EQUAL, 3);		
	}
	
	@Test
	public void equalsError() throws IllegalCharException, IllegalNumberException{
		thrown.expect(IllegalCharException.class);	
		String input="=//";
		Scanner scanner = new Scanner(input);
		scanner.scan();
	}
	
	@Test
	public void lineNumber() throws IllegalCharException, IllegalNumberException{
		String input = "show\r\n hide \n move \n file";
		Scanner scanner = new Scanner(input);
		scanner.scan();
		Token t0 = checkNext(scanner,KW_SHOW,0);
		checkPos(scanner,t0,0,0);
		Token t1 = checkNext(scanner,KW_HIDE,7);
		checkPos(scanner,t1,1,1);
		Token t2 = checkNext(scanner,KW_MOVE,14);
		checkPos(scanner,t2,2,1);
		Token t3 = checkNext(scanner,KW_FILE,21);
		checkPos(scanner,t3,3,1);
	}
	
	@Test
	public void testComma() throws IllegalCharException, IllegalNumberException, SyntaxException {
		String input = "  (3,5) ";
		Scanner scanner = new Scanner(input);
		scanner.scan();		
		checkNext(scanner,LPAREN,2);
		scanner.nextToken();
		checkNext(scanner,COMMA,4);
		scanner.nextToken();
		checkNext(scanner,RPAREN,6);
	}
	
	@Test
	public void testAssign() throws IllegalCharException, IllegalNumberException{
		String input = "  -< <- <+ <= <\n-<--";		
		Scanner scanner = new Scanner(input);
		scanner.scan();	
		checkNext(scanner,MINUS);
		checkNext(scanner,LT);
		checkNext(scanner,ASSIGN);
		checkNext(scanner,LT);
		checkNext(scanner,PLUS);
		checkNext(scanner,LE);
		checkNext(scanner,LT);
		checkNext(scanner,MINUS);
		checkNext(scanner,ASSIGN);
		checkNext(scanner,MINUS);
	}
}
