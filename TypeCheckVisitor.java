package cop5556sp17;

import cop5556sp17.AST.ASTNode;
import cop5556sp17.AST.ASTVisitor;
import cop5556sp17.AST.Tuple;
import cop5556sp17.AST.Type;
import cop5556sp17.AST.AssignmentStatement;
import cop5556sp17.AST.BinaryChain;
import cop5556sp17.AST.BinaryExpression;
import cop5556sp17.AST.Block;
import cop5556sp17.AST.BooleanLitExpression;
import cop5556sp17.AST.Chain;
import cop5556sp17.AST.ChainElem;
import cop5556sp17.AST.ConstantExpression;
import cop5556sp17.AST.Dec;
import cop5556sp17.AST.Expression;
import cop5556sp17.AST.FilterOpChain;
import cop5556sp17.AST.FrameOpChain;
import cop5556sp17.AST.IdentChain;
import cop5556sp17.AST.IdentExpression;
import cop5556sp17.AST.IdentLValue;
import cop5556sp17.AST.IfStatement;
import cop5556sp17.AST.ImageOpChain;
import cop5556sp17.AST.IntLitExpression;
import cop5556sp17.AST.ParamDec;
import cop5556sp17.AST.Program;
import cop5556sp17.AST.SleepStatement;
import cop5556sp17.AST.Statement;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.WhileStatement;

import java.util.ArrayList;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.LinePos;
import cop5556sp17.Scanner.Token;
import static cop5556sp17.AST.Type.TypeName.*;
import static cop5556sp17.Scanner.Kind.ARROW;
import static cop5556sp17.Scanner.Kind.KW_HIDE;
import static cop5556sp17.Scanner.Kind.KW_MOVE;
import static cop5556sp17.Scanner.Kind.KW_SHOW;
import static cop5556sp17.Scanner.Kind.KW_XLOC;
import static cop5556sp17.Scanner.Kind.KW_YLOC;
import static cop5556sp17.Scanner.Kind.OP_BLUR;
import static cop5556sp17.Scanner.Kind.OP_CONVOLVE;
import static cop5556sp17.Scanner.Kind.OP_GRAY;
import static cop5556sp17.Scanner.Kind.OP_HEIGHT;
import static cop5556sp17.Scanner.Kind.OP_WIDTH;
import static cop5556sp17.Scanner.Kind.*;

public class TypeCheckVisitor implements ASTVisitor {

	@SuppressWarnings("serial")
	public static class TypeCheckException extends Exception {
		TypeCheckException(String message) {
			super(message);
		}
	}

	SymbolTable symtab = new SymbolTable();

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
		// TODO Auto-generated method stub
		callCorrespondingChain(binaryChain.getE0(), arg);
		callCorrespondingChain(binaryChain.getE1(), arg);

		String arrow = binaryChain.getArrow().getText();
		Token first = binaryChain.getE1().getFirstToken();
		TypeName type = binaryChain.getE1().getType();

		switch (binaryChain.getE0().getType()) {
		case URL:
		case FILE:
			if (arrow.equals("->") && type == TypeName.IMAGE) {
				binaryChain.setTypeName(IMAGE);
			} else {
				throw new TypeCheckException(
						"Type Mismatch in Binary Chain - Invalid OP / CHAINELEM type for URL / FILE");
			}
			break;
		case FRAME:
			if (arrow.equals("->") && (first.isKind(KW_XLOC) || first.isKind(KW_YLOC))) {
				binaryChain.setTypeName(INTEGER);
			} else if (arrow.equals("->")
					&& (first.isKind(KW_SHOW) || first.isKind(KW_HIDE) || first.isKind(KW_MOVE))) {
				binaryChain.setTypeName(FRAME);
			} else {
				throw new TypeCheckException("Type Mismatch in Binary Chain - Invalid OP / CHAINELEM type for FRAME");
			}
			break;
		case IMAGE:
			if (arrow.equals("->") && binaryChain.getE1() instanceof ImageOpChain
					&& (first.isKind(OP_WIDTH) || first.isKind(OP_HEIGHT))) {
				binaryChain.setTypeName(INTEGER);
			} else if (arrow.equals("->") && (type == TypeName.FRAME)) {
				binaryChain.setTypeName(FRAME);
			} else if (arrow.equals("->") && (type == TypeName.FILE)) {
				binaryChain.setTypeName(NONE);
			} else if ((arrow.equals("->") || arrow.equals("|->")) && binaryChain.getE1() instanceof FilterOpChain
					&& (first.isKind(OP_GRAY) || first.isKind(OP_BLUR) || first.isKind(OP_CONVOLVE))) {
				binaryChain.setTypeName(IMAGE);
			} else if (arrow.equals("->") && binaryChain.getE1() instanceof ImageOpChain && first.isKind(KW_SCALE)) {
				binaryChain.setTypeName(IMAGE);
			} else if (arrow.equals("->") && binaryChain.getE1() instanceof IdentChain && binaryChain.getE1().getType() == TypeName.IMAGE) {
				binaryChain.setTypeName(IMAGE);
			} else if (arrow.equals("->") && binaryChain.getE1() instanceof IdentChain && binaryChain.getE1().getType() == TypeName.INTEGER) {
				binaryChain.setTypeName(IMAGE);
			} else {
				throw new TypeCheckException("Type Mismatch in Binary Chain - Invalid OP / CHAINELEM type for IMAGE");
			}
			break;
		case INTEGER:
			if (arrow.equals("->") && binaryChain.getE1() instanceof IdentChain && binaryChain.getE1().getType() == TypeName.INTEGER) {
				binaryChain.setTypeName(INTEGER);
			} else {
				throw new TypeCheckException(
						"Type Mismatch in Binary Chain - Invalid OP / CHAINELEM type for INTEGER");
			}
			break;
		default: {
			throw new TypeCheckException("Type Mismatch in Binary Chain - Invalid CHAIN type.");
		}
		}

		return null;
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
		// TODO Auto-generated method stub
		callCorrespondingExpression(binaryExpression.getE0(), arg);
		callCorrespondingExpression(binaryExpression.getE1(), arg);

		TypeName typeE0 = binaryExpression.getE0().getType();
		TypeName typeE1 = binaryExpression.getE1().getType();

		switch (binaryExpression.getOp().getText()) {
		case "+":
		case "-":
			if (typeE0 == TypeName.INTEGER && typeE1 == TypeName.INTEGER) {
				binaryExpression.setTypeName(INTEGER);
			} else if (typeE0 == TypeName.IMAGE && typeE1 == TypeName.IMAGE) {
				binaryExpression.setTypeName(IMAGE);
			} else {
				throw new TypeCheckException("PLUS/MINUS operated on other than INTEGER or IMAGE.");
			}
			break;

		case ">":
		case "<":
		case ">=":
		case "<=":
			if (typeE0 == TypeName.INTEGER && typeE1 == TypeName.INTEGER) {
				binaryExpression.setTypeName(BOOLEAN);
			} else if (typeE0 == TypeName.BOOLEAN && typeE1 == TypeName.BOOLEAN) {
				binaryExpression.setTypeName(BOOLEAN);
			} else {
				throw new TypeCheckException("LE/GE/GT/LT operated on other than INTEGER or BOOLEAN.");
			}
			break;
		case "==":
		case "!=":
			if (typeE0 == typeE1) {
				binaryExpression.setTypeName(BOOLEAN);
			} else {
				throw new TypeCheckException("EQUAL/NOT EQUAL operated on expressions of different types.");
			}
			break;
		case "*":
			if (typeE0 == TypeName.INTEGER && typeE1 == TypeName.INTEGER) {
				binaryExpression.setTypeName(INTEGER);
			} else if (typeE0 == TypeName.INTEGER && typeE1 == TypeName.IMAGE) {
				binaryExpression.setTypeName(IMAGE);
			} else if (typeE0 == TypeName.IMAGE && typeE1 == TypeName.INTEGER) {
				binaryExpression.setTypeName(IMAGE);
			} else {
				throw new TypeCheckException("TIMES operated on invalid combination of INTEGER or IMAGE.");
			}
			break;
		case "/":
			if (typeE0 == TypeName.INTEGER && typeE1 == TypeName.INTEGER) {
				binaryExpression.setTypeName(INTEGER);
			} 
			else if (typeE0 == TypeName.IMAGE && typeE1 == TypeName.INTEGER) {
				binaryExpression.setTypeName(IMAGE);
			}else {
				throw new TypeCheckException("DIV operated on other than INTEGER or IMAGE");
			}
			break;
		case "&":
			if (typeE0 == TypeName.BOOLEAN && typeE1 == TypeName.BOOLEAN) {
				binaryExpression.setTypeName(BOOLEAN);
			} else {
				throw new TypeCheckException("AND operated on other than BOOLEAN.");
			}
			break;
		case "|":
			if (typeE0 == TypeName.BOOLEAN && typeE1 == TypeName.BOOLEAN) {
				binaryExpression.setTypeName(BOOLEAN);
			} else {
				throw new TypeCheckException("OR operated on other than BOOLEAN.");
			}
			break;
		case "%":
			if (typeE0 == TypeName.INTEGER && typeE1 == TypeName.INTEGER) {
				binaryExpression.setTypeName(INTEGER);
			} else if (typeE0 == TypeName.IMAGE && typeE1 == TypeName.INTEGER) {
				binaryExpression.setTypeName(IMAGE);
			} else {
				throw new TypeCheckException("MOD operated on other than INTEGER or IMAGE.");
			}
			break;
		default: {
			throw new TypeCheckException("Invalid operand found in Binary Expression.");
		}
		}
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		// TODO Auto-generated method stub
		symtab.enterScope();
		for (Dec dec : block.getDecs()) {
			visitDec(dec, arg);
		}
		for (Statement st : block.getStatements()) {
			// visit expression
			if (st instanceof SleepStatement) {
				visitSleepStatement((SleepStatement) st, arg);
			} else if (st instanceof WhileStatement) {
				visitWhileStatement((WhileStatement) st, arg);
			} else if (st instanceof IfStatement) {
				visitIfStatement((IfStatement) st, arg);
			} else if (st instanceof AssignmentStatement) {
				visitAssignmentStatement((AssignmentStatement) st, arg);
			} else if (st instanceof Chain) {
				callCorrespondingChain(st, arg);
			}
		}
		symtab.leaveScope();
		return null;
	}

	private void callCorrespondingChain(Statement st, Object arg) throws Exception {
		// TODO Auto-generated method stub
		if (st instanceof IdentChain) {
			visitIdentChain((IdentChain) st, arg);
		} else if (st instanceof FilterOpChain) {
			visitFilterOpChain((FilterOpChain) st, arg);
		} else if (st instanceof FrameOpChain) {
			visitFrameOpChain((FrameOpChain) st, arg);
		} else if (st instanceof ImageOpChain) {
			visitImageOpChain((ImageOpChain) st, arg);
		} else if (st instanceof BinaryChain) {
			visitBinaryChain((BinaryChain) st, arg);
		}

	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		// TODO Auto-generated method stub
		booleanLitExpression.setTypeName(BOOLEAN);
		return null;
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitTuple(filterOpChain.getArg(), arg);
		if (filterOpChain.getArg().getExprList().size() != 0)
			throw new TypeCheckException("Tuple length found non zero for FilterOp");

		filterOpChain.setTypeName(IMAGE);
		return null;
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitTuple(frameOpChain.getArg(), arg);

		if (frameOpChain.getFirstToken().isKind(KW_SHOW) || frameOpChain.getFirstToken().isKind(KW_HIDE)) {
			if (frameOpChain.getArg().getExprList().size() == 0)
				frameOpChain.setTypeName(NONE);
			else
				throw new TypeCheckException("show or hide . tuple non zero length");
		}

		else if (frameOpChain.getFirstToken().isKind(KW_XLOC) || frameOpChain.getFirstToken().isKind(KW_YLOC)) {
			if (frameOpChain.getArg().getExprList().size() == 0)
				frameOpChain.setTypeName(INTEGER);
			else
				throw new TypeCheckException("xloc or yloc . tuple non zero length");
		}

		else if (frameOpChain.getFirstToken().isKind(KW_MOVE)) {
			if (frameOpChain.getArg().getExprList().size() == 2)
				frameOpChain.setTypeName(NONE);
			else {
				throw new TypeCheckException("move . tuple non 2 length");
			}
		} else {
			throw new TypeCheckException("FrameOp invalid first token.");
		}

		return null;
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
		// TODO Auto-generated method stub

		Dec dec = symtab.lookup(identChain.getFirstToken().getText());
		if (dec != null)
		{	identChain.setTypeName(dec.getTypeName());
			identChain.setDec(dec);
		}
		else
			throw new TypeCheckException(identChain.getFirstToken().getText() + " found undeclared for current scope.");

		return null;
	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Dec dec = symtab.lookup(identExpression.getFirstToken().getText());
		if (dec != null)
		{
			identExpression.setTypeName(dec.getTypeName());
			identExpression.setDec(dec);
		}
		else
			throw new TypeCheckException(identExpression.getFirstToken().getText() + " undeclared for current scope.");

		return null;
	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		// TODO Auto-generated method stub
		callCorrespondingExpression(ifStatement.getE(), arg);

		visitBlock(ifStatement.getB(), arg);

		if (ifStatement.getE().getType() != TypeName.BOOLEAN) {
			throw new TypeCheckException("Expression not boolean for IF");
		}

		return null;
	}

	private void callCorrespondingExpression(Expression e, Object arg) throws Exception {
		// TODO Auto-generated method stub

		// visit expression
		if (e instanceof BooleanLitExpression) {
			visitBooleanLitExpression((BooleanLitExpression) e, arg);
		} else if (e instanceof IdentExpression) {
			visitIdentExpression((IdentExpression) e, arg);
		} else if (e instanceof IntLitExpression) {
			visitIntLitExpression((IntLitExpression) e, arg);
		} else if (e instanceof ConstantExpression) {
			visitConstantExpression((ConstantExpression) e, arg);
		} else if (e instanceof BinaryExpression) {
			visitBinaryExpression((BinaryExpression) e, arg);
		} else {
			throw new TypeCheckException("Invalid expression instance found.");
		}
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		// TODO Auto-generated method stub
		intLitExpression.setTypeName(INTEGER);
		return null;
	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		// TODO Auto-generated method stub
		// visit expression
		callCorrespondingExpression(sleepStatement.getE(), arg);

		if (sleepStatement.getE().type != TypeName.INTEGER) {
			throw new TypeCheckException("Expression not integer for SLEEP.");
		}
		return null;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		// TODO Auto-generated method stub
		// visit expression
		callCorrespondingExpression(whileStatement.getE(), arg);

		// visit block
		visitBlock(whileStatement.getB(), arg);

		// condition
		if (whileStatement.getE().getType() != TypeName.BOOLEAN) {
			throw new TypeCheckException("Expression not boolean for WHILE.");
		}

		return null;
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		// TODO Auto-generated method stub

		declaration.setTypeName(Type.getTypeName(declaration.getFirstToken()));

		boolean insertSuccess = symtab.insert(declaration.getIdent().getText(), declaration);
		if (!insertSuccess) {
			throw new TypeCheckException(declaration.getIdent().getText() + " already defined in the current scope.");
		}

		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		// TODO Auto-generated method stub

		// Let ParamDecs be always in scope.
		symtab.enterScope();

		for (ParamDec paramdec : program.getParams()) {
			visitParamDec(paramdec, arg);
		}

		visitBlock(program.getB(), arg);
		symtab.leaveScope();
		//System.out.println(symtab.toString());
		return null;
	}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitIdentLValue(assignStatement.getVar(), arg);

		callCorrespondingExpression(assignStatement.getE(), arg);

		if (assignStatement.getVar().getDec().getTypeName() != assignStatement.getE().getType()) {
			throw new TypeCheckException("Type Mismatch in assignment statement.");
		}
		return null;
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Dec dec = symtab.lookup(identX.getText());
		if (dec != null)
			identX.setDec(dec);
		else
			throw new TypeCheckException(identX.getText() + " undeclared for current scope.");

		return null;
	}

	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		// TODO Auto-generated method stub

		paramDec.setTypeName(Type.getTypeName(paramDec.getFirstToken()));
		boolean insertSuccess = symtab.insert(paramDec.getIdent().getText(), paramDec);
		if (!insertSuccess) {
			throw new TypeCheckException(paramDec.getIdent().getText() + " already defined in the current scope.");
		}
		return null;
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
		// TODO Auto-generated method stub
		constantExpression.setTypeName(INTEGER);
		return null;
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		// TODO Auto-generated method stub
		visitTuple(imageOpChain.getArg(), arg);

		if (imageOpChain.getFirstToken().isKind(OP_WIDTH) || imageOpChain.getFirstToken().isKind(OP_HEIGHT)) {
			if (imageOpChain.getArg().getExprList().size() == 0)
				imageOpChain.setTypeName(INTEGER);
			else
				throw new TypeCheckException("width or height . tuple found of non-zero length");
		}

		else if (imageOpChain.getFirstToken().isKind(KW_SCALE)) {
			if (imageOpChain.getArg().getExprList().size() == 1)
				imageOpChain.setTypeName(IMAGE);
			else
				throw new TypeCheckException("scale . tuple found of non-one length");
		} else {
			throw new TypeCheckException("Invalid ImageOp found.");
		}
		return null;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		// TODO Auto-generated method stub
		for (Expression e : tuple.getExprList()) {
			callCorrespondingExpression(e, arg);
			if (e.getType() != TypeName.INTEGER)
				throw new TypeCheckException("Type Mismatch in Tuple - Expression not of type integer.");
		}
		return null;
	}

}
