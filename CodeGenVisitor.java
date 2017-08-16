package cop5556sp17;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;
import cop5556sp17.TypeCheckVisitor.TypeCheckException;
import cop5556sp17.AST.ASTVisitor;
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
import cop5556sp17.AST.Tuple;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.WhileStatement;
import cop5556sp17.PLPRuntimeFrame;
import cop5556sp17.PLPRuntimeFilterOps;

import static cop5556sp17.AST.Type.TypeName.BOOLEAN;
import static cop5556sp17.AST.Type.TypeName.FRAME;
import static cop5556sp17.AST.Type.TypeName.IMAGE;
import static cop5556sp17.AST.Type.TypeName.INTEGER;
import static cop5556sp17.AST.Type.TypeName.URL;
import static cop5556sp17.Scanner.Kind.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 */
	public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
		this.slotNumber = 1;
		this.iterator = 0;
	}

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;
	int slotNumber;
	int length;
	int iterator;

	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		// cw = new ClassWriter(0);
		className = program.getName();
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;

		cw.visit(51, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { "java/lang/Runnable" });
		cw.visitSource(sourceFileName, null);

		// generate constructor code
		// get a MethodVisitor
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null, null);
		mv.visitCode();

		// Create label at start of code
		Label constructorStart = new Label();
		mv.visitLabel(constructorStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering <init>");
		// generate code to call superclass constructor
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		// visit parameter decs to add each as field to the class
		// pass in mv so decs can add their initialization code to the
		// constructor.

		ArrayList<ParamDec> params = program.getParams();
		length = params.size();
		for (ParamDec dec : params)
			dec.visit(this, mv);
		mv.visitInsn(RETURN);
		// create label at end of code
		Label constructorEnd = new Label();
		mv.visitLabel(constructorEnd);
		// finish up by visiting local vars of constructor
		// the fourth and fifth arguments are the region of code where the local
		// variable is defined as represented by the labels we inserted.
		mv.visitLocalVariable("this", classDesc, null, constructorStart, constructorEnd, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, constructorStart, constructorEnd, 1);
		// indicates the max stack size for the method.
		// because we used the COMPUTE_FRAMES parameter in the classwriter
		// constructor, asm
		// will do this for us. The parameters to visitMaxs don't matter, but
		// the method must
		// be called.
		mv.visitMaxs(1, 1);
		// finish up code generation for this method.
		mv.visitEnd();
		// end of constructor

		// create main method which does the following
		// 1. instantiate an instance of the class being generated, passing the
		// String[] with command line arguments
		// 2. invoke the run method.
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		mv.visitCode();
		Label mainStart = new Label();
		mv.visitLabel(mainStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering main");
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "([Ljava/lang/String;)V", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V", false);
		mv.visitInsn(RETURN);
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		mv.visitLocalVariable("instance", classDesc, null, mainStart, mainEnd, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		// create run method
		mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		mv.visitCode();
		Label startRun = new Label();
		mv.visitLabel(startRun);
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering run");
		program.getB().visit(this, null);
		mv.visitInsn(RETURN);
		Label endRun = new Label();
		mv.visitLabel(endRun);
		mv.visitLocalVariable("this", classDesc, null, startRun, endRun, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, startRun, endRun, 1);

		// TODO visit the local variables
		mv.visitMaxs(1, 1);
		mv.visitEnd(); // end of run method

		cw.visitEnd();// end of class

		// generate classfile and return it
		return cw.toByteArray();
	}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		assignStatement.getE().visit(this, arg);
		CodeGenUtils.genPrint(DEVEL, mv, "\nassignment: " + assignStatement.var.getText() + "=");
		CodeGenUtils.genPrintTOS(GRADE, mv, assignStatement.getE().getType());
		assignStatement.getVar().visit(this, arg);
		return null;
	}

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
		Chain chainOnLeft = binaryChain.getE0();
		ChainElem chainElemOnRight = binaryChain.getE1();

		// visit left
		chainOnLeft.visit(this, true);

		if (chainOnLeft.getType() == TypeName.URL)
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "readFromURL",
					PLPRuntimeImageIO.readFromURLSig, false);

		else if (chainOnLeft.getType() == TypeName.FILE)
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "readFromFile",
					PLPRuntimeImageIO.readFromFileDesc, false);

		mv.visitInsn(DUP);

		// visit right
		chainElemOnRight.visit(this, false);

		return null;
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
		// TODO Implement this
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering binary exp e0 = " + binaryExpression.getE0().firstToken.getText()
				+ " e1 = " + binaryExpression.getE1().firstToken.getText());

		Label setTrue = new Label();
		Label done = new Label();

		TypeName t0 = binaryExpression.getE0().getType();
		TypeName t1 = binaryExpression.getE1().getType();

		binaryExpression.getE0().visit(this, arg);
		binaryExpression.getE1().visit(this, arg);

		switch (binaryExpression.getOp().getText()) {
		case "+":
			if (t0 == TypeName.INTEGER)
				mv.visitInsn(IADD);
			else if (t0 == TypeName.IMAGE)
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "add", PLPRuntimeImageOps.addSig, false);
			break;
		case "-":
			if (t0 == TypeName.INTEGER)
				mv.visitInsn(ISUB);
			else if (t0 == TypeName.IMAGE)
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "sub", PLPRuntimeImageOps.subSig, false);
			break;
		case "==":
			mv.visitJumpInsn(IF_ICMPEQ, setTrue);
			mv.visitLdcInsn(false);
			break;
		case "!=":
			mv.visitJumpInsn(IF_ICMPNE, setTrue);
			mv.visitLdcInsn(false);
			break;
		case "<":
			mv.visitJumpInsn(IF_ICMPLT, setTrue);
			mv.visitLdcInsn(false);
			break;
		case ">":
			mv.visitJumpInsn(IF_ICMPGT, setTrue);
			mv.visitLdcInsn(false);
			break;
		case ">=":
			mv.visitJumpInsn(IF_ICMPGE, setTrue);
			mv.visitLdcInsn(false);
			break;
		case "<=":
			mv.visitJumpInsn(IF_ICMPLE, setTrue);
			mv.visitLdcInsn(false);
			break;
		case "&":
			mv.visitInsn(IAND);
			break;
		case "|":
			mv.visitInsn(IOR);
			break;
		case "*":
			if (t0 == TypeName.INTEGER && t1 == TypeName.INTEGER)
				mv.visitInsn(IMUL);
			else {
				if (t0 == TypeName.INTEGER && t1 == TypeName.IMAGE)
					mv.visitInsn(SWAP);
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mul", PLPRuntimeImageOps.mulSig, false);
			}
			break;
		case "/":
			if (t0 == TypeName.INTEGER)
				mv.visitInsn(IDIV);
			else if (t0 == TypeName.IMAGE)
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "div", PLPRuntimeImageOps.divSig, false);
			break;
		case "%":
			if (t0 == TypeName.INTEGER)
				mv.visitInsn(IREM);
			else if (t0 == TypeName.IMAGE)
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mod", PLPRuntimeImageOps.modSig, false);
			break;

		default:
			break;

		}
		mv.visitJumpInsn(GOTO, done);

		mv.visitLabel(setTrue);
		mv.visitLdcInsn(true);

		mv.visitLabel(done);
		return null;

	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		// TODO Implement this
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering block");
		// local variables dec
		// statements executed in run method.

		// Create label at start of code
		Label blockStart = new Label();
		Label blockEnd = new Label();
		mv.visitLabel(blockStart);

		ArrayList<Dec> decs = block.getDecs();
		for (Dec dec : decs) {
			dec.visit(this, mv);
		}

		ArrayList<Statement> statements = block.getStatements();
		for (Statement statement : statements) {
			if (statement instanceof AssignmentStatement) {
				if (((AssignmentStatement) statement).getVar().getDec() instanceof ParamDec) {
					mv.visitVarInsn(ALOAD, 0);
				}
			}
			statement.visit(this, mv);

			if (statement instanceof BinaryChain)
				mv.visitInsn(POP);
		}

		// create label at end of code

		mv.visitLabel(blockEnd);

		for (Dec dec : decs) {
			mv.visitLocalVariable(dec.getIdent().getText(), dec.getTypeName().getJVMTypeDesc(), null, blockStart,
					blockEnd, dec.getSlot());
		}

		return null;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		// TODO Implement this
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering boollit");
		mv.visitLdcInsn(booleanLitExpression.getValue());
		return null;
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {

		if (constantExpression.getFirstToken().isKind(KW_SCREENWIDTH)) {
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenWidth",
					PLPRuntimeFrame.getScreenWidthSig, false);
		} else if (constantExpression.getFirstToken().isKind(KW_SCREENHEIGHT)) {
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenHeight",
					PLPRuntimeFrame.getScreenHeightSig, false);
		}
		return null;
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		// TODO Implement this

		CodeGenUtils.genPrint(DEVEL, mv, "\nentering dec");

		// assign to local variable. using slotNumber++;
		declaration.setSlot(slotNumber++);

		if (declaration.getTypeName() == TypeName.IMAGE || declaration.getTypeName() == TypeName.FRAME) {
			mv.visitInsn(ACONST_NULL);
			mv.visitVarInsn(ASTORE, declaration.getSlot());
		}
		return null;
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {

		if (filterOpChain.getFirstToken().isKind(OP_CONVOLVE)) {
			mv.visitInsn(ACONST_NULL);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "convolveOp", PLPRuntimeFilterOps.opSig,
					false);
		} else if (filterOpChain.getFirstToken().isKind(OP_BLUR)) {
			mv.visitInsn(ACONST_NULL);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "blurOp", PLPRuntimeFilterOps.opSig, false);
		} else if (filterOpChain.getFirstToken().isKind(OP_GRAY)) {
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "grayOp", PLPRuntimeFilterOps.opSig, false);
		}

		return null;
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		switch (frameOpChain.getFirstToken().getText()) {
		case "xloc":
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getXVal", PLPRuntimeFrame.getXValDesc,
					false);
			break;
		case "yloc":
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getYVal", PLPRuntimeFrame.getYValDesc,
					false);
			break;
		case "show":
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "showImage", PLPRuntimeFrame.showImageDesc,
					false);
			break;
		case "hide":
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "hideImage", PLPRuntimeFrame.hideImageDesc,
					false);
			break;
		case "move":
			frameOpChain.getArg().visit(this, arg);
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "moveFrame", PLPRuntimeFrame.moveFrameDesc,
					false);
			break;
		}
		return null;
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {

		Boolean left = (Boolean) arg;
		Dec dec = identChain.getDec();
		String var = dec.getIdent().getText();

		if (left) {

			if (identChain.getDec() instanceof ParamDec) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, className, var, dec.getTypeName().getJVMTypeDesc());
			} else {
				if (identChain.getType() == TypeName.INTEGER || identChain.getType() == TypeName.BOOLEAN)
					mv.visitVarInsn(ILOAD, dec.getSlot());
				else
					mv.visitVarInsn(ALOAD, dec.getSlot());
			}

		} else {

			if (identChain.getDec() instanceof ParamDec) {

				if (identChain.getType() == TypeName.FILE) {
					mv.visitInsn(POP);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, var, dec.getTypeName().getJVMTypeDesc());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "write",
							PLPRuntimeImageIO.writeImageDesc, false);
				} else {
					mv.visitVarInsn(ALOAD, 0);
					mv.visitInsn(SWAP);
					mv.visitFieldInsn(PUTFIELD, className, var, dec.getTypeName().getJVMTypeDesc());
				}

			} else {

				if (dec.getTypeName() == TypeName.INTEGER) {
					mv.visitVarInsn(ISTORE, dec.getSlot());
				} else if (dec.getTypeName() == TypeName.IMAGE) {
					mv.visitVarInsn(ASTORE, dec.getSlot());
				} else if (dec.getTypeName() == TypeName.FRAME) {
					mv.visitInsn(POP);
					mv.visitVarInsn(ALOAD, dec.getSlot());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "createOrSetFrame",
							PLPRuntimeFrame.createOrSetFrameSig, false);
					mv.visitInsn(DUP);
					mv.visitVarInsn(ASTORE, dec.getSlot());
				} else if (dec.getTypeName() == TypeName.FILE) {
					mv.visitInsn(POP);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, identChain.getDec().getIdent().getText(),
							identChain.getDec().getTypeName().getJVMTypeDesc());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "write",
							PLPRuntimeImageIO.writeImageDesc, false);
				}
			}
		}
		return null;
	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		// TODO Implement this
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering ident exp");

		if (identExpression.getDec() instanceof ParamDec) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, className, identExpression.getFirstToken().getText(),
					identExpression.getType().getJVMTypeDesc());
		} else {
			if (identExpression.getType() == TypeName.INTEGER || identExpression.getType() == TypeName.BOOLEAN)
				mv.visitVarInsn(ILOAD, identExpression.getDec().getSlot());
			else
				mv.visitVarInsn(ALOAD, identExpression.getDec().getSlot());
		}

		return null;
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		// TODO Implement this

		if (identX.getDec() instanceof ParamDec) {

			if (identX.getDec().getTypeName() == TypeName.INTEGER)
				mv.visitFieldInsn(PUTFIELD, className, identX.getText(), "I");
			else if (identX.getDec().getTypeName() == TypeName.BOOLEAN)
				mv.visitFieldInsn(PUTFIELD, className, identX.getText(), "Z");
			else {
				mv.visitFieldInsn(PUTFIELD, className, identX.getText(),
						identX.getDec().getTypeName().getJVMTypeDesc());
			}
		} else {
			if (identX.getDec().getTypeName() == TypeName.INTEGER || identX.getDec().getTypeName() == TypeName.BOOLEAN)
				mv.visitVarInsn(ISTORE, identX.getDec().getSlot());
			else if (identX.getDec().getTypeName() == TypeName.IMAGE) {
				mv.visitInsn(DUP);
				mv.visitVarInsn(ASTORE, identX.getDec().getSlot());
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "copyImage",
						PLPRuntimeImageOps.copyImageSig, false);
			} else {
				mv.visitVarInsn(ASTORE, identX.getDec().getSlot());
			}
		}

		return null;
	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		// TODO Implement this
		Label after = new Label();

		ifStatement.getE().visit(this, arg);
		mv.visitJumpInsn(IFEQ, after);
		ifStatement.getB().visit(this, arg);
		mv.visitLabel(after);

		return null;
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {

		if (imageOpChain.getFirstToken().isKind(KW_SCALE)) {
			imageOpChain.getArg().visit(this, arg);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "scale", PLPRuntimeImageOps.scaleSig, false);
		} else if (imageOpChain.getFirstToken().isKind(OP_WIDTH)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeImageIO.BufferedImageClassName, "getWidth",
					PLPRuntimeImageOps.getWidthSig, false);
		} else if (imageOpChain.getFirstToken().isKind(OP_HEIGHT)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeImageIO.BufferedImageClassName, "getHeight",
					PLPRuntimeImageOps.getHeightSig, false);
		}

		return null;
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		// TODO Implement this
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering intLit . Pushing : " + intLitExpression.value);

		mv.visitLdcInsn(intLitExpression.value);
		return null;
	}

	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		// TODO Implement this
		// Visit fields for each paramDec
		if (paramDec.getTypeName() == TypeName.INTEGER) {
			FieldVisitor fv = cw.visitField(ACC_PUBLIC, paramDec.getIdent().getText(), "I", null, null);
			fv.visitEnd();
		} else if (paramDec.getTypeName() == TypeName.BOOLEAN) {
			FieldVisitor fv = cw.visitField(ACC_PUBLIC, paramDec.getIdent().getText(), "Z", null, null);
			fv.visitEnd();
		} else if (paramDec.getTypeName() == TypeName.FILE) {
			FieldVisitor fv = cw.visitField(ACC_PUBLIC, paramDec.getIdent().getText(), PLPRuntimeImageIO.FileDesc, null,
					null);
			fv.visitEnd();
		} else if (paramDec.getTypeName() == TypeName.URL) {
			FieldVisitor fv = cw.visitField(ACC_PUBLIC, paramDec.getIdent().getText(), PLPRuntimeImageIO.URLDesc, null,
					null);
			fv.visitEnd();
		}

		// For assignment 5, only needs to handle integers and booleans
		mv.visitVarInsn(ALOAD, 0); // load this pointer

		if (paramDec.getTypeName() == TypeName.INTEGER) {
			mv.visitVarInsn(ALOAD, 1); // arg string ref added on top of stack
			mv.visitLdcInsn(iterator++); // push index of arg to map
			mv.visitInsn(AALOAD); // index pop, ref pop, pushes ref[index]
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
			mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), "I");
		} else if (paramDec.getTypeName() == TypeName.BOOLEAN) {
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(iterator++);
			mv.visitInsn(AALOAD);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
			mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), "Z");
		} else if (paramDec.getTypeName() == TypeName.URL) {
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(iterator++);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "getURL", PLPRuntimeImageIO.getURLSig, false);
			mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), PLPRuntimeImageIO.URLDesc);
		} else if (paramDec.getTypeName() == TypeName.FILE) {
			mv.visitTypeInsn(NEW, "java/io/File");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(iterator++);
			mv.visitInsn(AALOAD);
			mv.visitMethodInsn(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false);
			mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), PLPRuntimeImageIO.FileDesc);
		}

		return null;
	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		sleepStatement.getE().visit(this, arg);
		mv.visitInsn(I2L);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);
		return null;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {

		for (Expression exp : tuple.getExprList()) {

			exp.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		// TODO Implement this
		Label guard = new Label();
		Label body = new Label();

		mv.visitJumpInsn(GOTO, guard);

		mv.visitLabel(body);
		whileStatement.getB().visit(this, null);

		mv.visitLabel(guard);
		whileStatement.getE().visit(this, arg);

		mv.visitJumpInsn(IFNE, body);

		return null;
	}

}
