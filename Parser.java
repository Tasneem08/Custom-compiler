package cop5556sp17;

import cop5556sp17.Scanner.Kind;
import static cop5556sp17.Scanner.Kind.*;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.*;

public class Parser {

	/**
	 * Exception to be thrown if a syntax error is detected in the input. You
	 * will want to provide a useful error message.
	 *
	 */
	@SuppressWarnings("serial")
	public static class SyntaxException extends Exception {
		public SyntaxException(String message) {
			super(message);
		}
	}

	/**
	 * Useful during development to ensure unimplemented routines are not
	 * accidentally called during development. Delete it when the Parser is
	 * finished.
	 *
	 */
	@SuppressWarnings("serial")
	public static class UnimplementedFeatureException extends RuntimeException {
		public UnimplementedFeatureException() {
			super();
		}
	}

	Scanner scanner;
	Token t;

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
		// map first token to the AST node ka structure.

	}

	/**
	 * parse the input using tokens from the scanner. Check for EOF (i.e. no
	 * trailing junk) when finished
	 * 
	 * @throws SyntaxException
	 */
	ASTNode parse() throws SyntaxException {
		Program p = program();
		matchEOF();
		return p;
	}

	private static Set<Kind> firstParamDec = new HashSet<Kind>(Arrays.asList(KW_URL, KW_INTEGER, KW_FILE, KW_BOOLEAN));
	private static Set<Kind> firstDec = new HashSet<Kind>(Arrays.asList(KW_INTEGER, KW_BOOLEAN, KW_IMAGE, KW_FRAME));
	private static Set<Kind> firstStatement = new HashSet<Kind>(Arrays.asList(OP_SLEEP, KW_WHILE, KW_IF, IDENT));
	private static Set<Kind> filterOp = new HashSet<Kind>(Arrays.asList(OP_BLUR, OP_GRAY, OP_CONVOLVE));
	private static Set<Kind> frameOp = new HashSet<Kind>(Arrays.asList(KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC));
	private static Set<Kind> imageOp = new HashSet<Kind>(Arrays.asList(OP_WIDTH, OP_HEIGHT, KW_SCALE));
	private static Set<Kind> factor = new HashSet<Kind>(
			Arrays.asList(IDENT, INT_LIT, KW_TRUE, KW_FALSE, KW_SCREENWIDTH, KW_SCREENHEIGHT));
	private static Set<Kind> relOp = new HashSet<Kind>(Arrays.asList(LT, LE, GT, GE, EQUAL, NOTEQUAL));
	private static Set<Kind> weakOp = new HashSet<Kind>(Arrays.asList(PLUS, MINUS, OR));
	private static Set<Kind> strongOp = new HashSet<Kind>(Arrays.asList(TIMES, DIV, AND, MOD));

	Expression expression() throws SyntaxException {
		Token first = t;
		Expression e0 = null;
		Expression e1 = null;
		e0 = term();
		while (relOp.contains(t.kind)) {
			Token op = t;
			consume();
			e1 = term();
			e0 = new BinaryExpression(first, e0, op, e1);
		}
		return e0;
	}

	Expression term() throws SyntaxException {
		Token first = t;
		Expression e0 = null;
		Expression e1 = null;
		e0 = elem();
		while (weakOp.contains(t.kind)) {
			Token op = t;
			consume();
			e1 = elem();
			e0 = new BinaryExpression(first, e0, op, e1);
		}
		return e0;
	}

	Expression elem() throws SyntaxException {
		Token first = t;
		Expression e0 = null;
		Expression e1 = null;
		e0 = factor();
		while (strongOp.contains(t.kind)) {
			Token op = t;
			consume();
			e1 = factor();
			e0 = new BinaryExpression(first, e0, op, e1);
		}
		return e0;
	}

	Expression factor() throws SyntaxException {
		Token first = t;
		Expression e = null;
		if (factor.contains(t.kind)) {
			switch (t.kind) {
			case KW_TRUE:
			case KW_FALSE:
				e = new BooleanLitExpression(first);
				break;
			case KW_SCREENWIDTH:
			case KW_SCREENHEIGHT:
				e = new ConstantExpression(first);
				break;
			case INT_LIT:
				e = new IntLitExpression(first);
				break;
			case IDENT:
				e = new IdentExpression(first);
				break;
			default:
				break;
			}
			consume();
		} else if (t.kind == Kind.LPAREN) {
			consume();
			e = expression();
			match(RPAREN);
		} else
			throw new SyntaxException("Illegal Factor: " + t.kind + " found at " + t.getLinePos().toString()
					+ "Expected <IDENT> or <INT_LIT> or <KW_TRUE> or <KW_FALSE> or <KW_SCREENWIDTH> or <KW_SCREENHEIGHT> or <( expression )> instead.");
		return e;
	}

	Block block() throws SyntaxException {
		Token first = t;
		ArrayList<Dec> ld = new ArrayList<Dec>();
		ArrayList<Statement> ls = new ArrayList<Statement>();

		if (t.isKind(LBRACE)) {
			consume();
			while (firstDec.contains(t.kind) || firstStatement.contains(t.kind) || filterOp.contains(t.kind)
					|| frameOp.contains(t.kind) || imageOp.contains(t.kind)) {
				if (firstStatement.contains(t.kind) || filterOp.contains(t.kind) || frameOp.contains(t.kind)
						|| imageOp.contains(t.kind))
					ls.add(statement());
				else
					ld.add(dec());
			}

			match(RBRACE);
		} else
			throw new SyntaxException("Illegal Block: " + t.kind + " found at " + t.getLinePos().toString()
					+ " Expected <LBRACE> instead.");

		return new Block(first, ld, ls);

	}

	Program program() throws SyntaxException {
		Token first = t;
		ArrayList<ParamDec> pd = new ArrayList<ParamDec>();
		Program p = null;
		if (t.isKind(IDENT)) {
			consume();
			if (t.isKind(LBRACE)) {
				p = new Program(first, pd, block());
			} else {
				pd.add(paramDec());
				while (t.isKind(COMMA)) {
					consume();
					pd.add(paramDec());
				}
				p = new Program(first, pd, block());
			}
		} else {
			throw new SyntaxException("Illegal Program: " + t.kind + " found at " + t.getLinePos().toString()
					+ " Expected <IDENT> instead.");
		}

		return p;
	}

	ParamDec paramDec() throws SyntaxException {
		Token first = t;
		ParamDec pd = null;
		if (firstParamDec.contains(t.kind)) {

			consume();
			pd = new ParamDec(first, t);
			match(IDENT);
		}

		else {
			pd = null;
			throw new SyntaxException("Illegal ParamDec: " + t.kind + " found at " + t.getLinePos().toString()
					+ " Expected " + firstParamDec + " instead.");
		}

		return pd;
	}

	Dec dec() throws SyntaxException {
		Dec d = null;
		if (firstDec.contains(t.kind)) {
			Token first = t;
			consume();
			d = new Dec(first, t);
			match(IDENT);

		} else {
		

			throw new SyntaxException("Illegal Dec: " + t.kind + "found at " + t.getLinePos().toString() + " Expected "
					+ firstDec + " instead.");
		}
		return d;
	}

	Statement statement() throws SyntaxException {
		Token nextToken = scanner.peek();
		Expression e = null;
		Token first = t;
		Statement s = null;
		if (first.isKind(OP_SLEEP)) {
			consume();
			e = expression();
			s = new SleepStatement(first, e);
			match(SEMI);
		} else if (first.isKind(KW_WHILE)) {
			consume();
			match(LPAREN);
			e = expression();
			match(RPAREN);
			s = new WhileStatement(first, e, block());
		} else if (first.isKind(KW_IF)) {
			consume();
			match(LPAREN);
			e = expression();
			match(RPAREN);
			s = new IfStatement(first, e, block());
		}

		else if (first.isKind(IDENT) && (nextToken.isKind(ASSIGN))) {
			IdentLValue lval = new IdentLValue(first);
			consume();
			consume();
			e = expression();
			s = new AssignmentStatement(first, lval, e);
			match(SEMI);
		}

		else if (t.isKind(IDENT) || filterOp.contains(t.kind) || frameOp.contains(t.kind) || imageOp.contains(t.kind)) {
			s = chain();
			match(SEMI);
		} else
			throw new SyntaxException("Illegal Statement: " + t.kind + " found at " + t.getLinePos().toString()
					+ " Expected <OP_SLEEP> or <KW_WHILE> or <KW_IF> or <IDENT> instead.");
		return s;
	}

	Chain chain() throws SyntaxException {
		Token first = t;
		Chain c0 = chainElem();
		ChainElem c1 = null;
		Token arrow = t;
		match(new Kind[] { Kind.ARROW, Kind.BARARROW });
		c1 = chainElem();
		c0 = new BinaryChain(first, c0, arrow, c1);
		while (t.isKind(ARROW) || t.isKind(BARARROW)) {
			arrow = t;
			consume();
			c1 = chainElem();
			c0 = new BinaryChain(first, c0, arrow, c1);
		}
		return c0;
	}

	ChainElem chainElem() throws SyntaxException {
		Tuple tuple = null;
		ChainElem ce = null;
		Token first = t;

		if (first.isKind(IDENT)) {
			ce = new IdentChain(t);
			consume();
		} else if (filterOp.contains(first.kind)) {
			consume();
			tuple = arg();
			ce = new FilterOpChain(first, tuple);
		} else if (frameOp.contains(first.kind)) {
			consume();
			tuple = arg();
			ce = new FrameOpChain(first, tuple);
		} else if (imageOp.contains(first.kind)) {
			consume();
			tuple = arg();
			ce = new ImageOpChain(first, tuple);
		}

		else
			throw new SyntaxException("Illegal ChainElem: " + t.kind + " found at " + t.getLinePos().toString()
					+ " Expected <IDENT> or <filterOp arg> or <frameOp arg> or <imageOp arg> instead.");

		return ce;
	}

	Tuple arg() throws SyntaxException {
		Token first = t;
		List<Expression> le = new ArrayList<Expression>();
		if (t.isKind(LPAREN)) {
			consume();
			le.add(expression());
			while (t.isKind(COMMA)) {
				consume();
				le.add(expression());

			}
			match(RPAREN);

		}
		return new Tuple(first, le);
	}

	/**
	 * Checks whether the current token is the EOF token. If not, a
	 * SyntaxException is thrown.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (t.isKind(EOF)) {
			return t;
		}
		throw new SyntaxException(
				"Illegal EOF: " + t.kind + " found at " + t.getLinePos().toString() + " Expected <EOF> instead.");
	}

	/**
	 * Checks if the current token has the given kind. If so, the current token
	 * is consumed and returned. If not, a SyntaxException is thrown.
	 * 
	 * Precondition: kind != EOF
	 * 
	 * @param kind
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind kind) throws SyntaxException {
		if (t.isKind(kind)) {
			return consume();
		}
		throw new SyntaxException("Illegal Match: " + t.kind.getText() + " found at " + t.getLinePos().toString()
				+ " Expected " + kind.getText() + " instead.");
	}

	/**
	 * Checks if the current token has one of the given kinds. If so, the
	 * current token is consumed and returned. If not, a SyntaxException is
	 * thrown.
	 * 
	 * * Precondition: for all given kinds, kind != EOF
	 * 
	 * @param kinds
	 *            list of kinds, matches any one
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind... kinds) throws SyntaxException {
		Set<Kind> list = new HashSet<Kind>(Arrays.asList(kinds));
		if (list.contains(t.kind))
			return consume();
		else
			throw new SyntaxException("Illegal Match: " + t.kind + " found at " + t.getLinePos().toString()
					+ " Expected " + list + " instead.");
	}

	/**
	 * Gets the next token and returns the consumed token.
	 * 
	 * Precondition: t.kind != EOF
	 * 
	 * @return
	 * 
	 */
	private Token consume() throws SyntaxException {
		Token tmp = t;
		t = scanner.nextToken();
		return tmp;
	}

}
