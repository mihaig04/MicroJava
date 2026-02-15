package ssw.mj.impl;

import ssw.mj.Errors.Message;
import ssw.mj.codegen.Label;
import ssw.mj.codegen.Operand;
import ssw.mj.scanner.Token;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.scanner.Token.Kind.*;

public final class Parser {

  /**
   * Maximum number of global variables per program
   */
  private static final int MAX_GLOBALS = 32767;

  /**
   * Maximum number of fields per class
   */
  private static final int MAX_FIELDS = 32767;

  /**
   * Maximum number of local variables per method
   */
  private static final int MAX_LOCALS = 127;

  private static final int MIN_ERROR_DIST = 3;

  private static final BitSet firstStatement;
  private static final BitSet firstFactor;
  private static final BitSet firstAssignop;
  private static final BitSet firstMulop;

  private static final EnumSet<Token.Kind> firstRecoverGlobalDeclSet;
  private static final EnumSet<Token.Kind> breakRecoverGlobalDeclSet;
  private static final EnumSet<Token.Kind> firstRecoverMethodDeclSet;
  private static final EnumSet<Token.Kind> breakRecoverMethodDeclSet;
  private static final EnumSet<Token.Kind> firstRecoverStatementSet;
  private static final EnumSet<Token.Kind> breakRecoverStatementSet;

  /**
   * Last recognized token;
   */
  private Token t;

  /**
   * Lookahead token (not recognized).)
   */
  private Token la;

  /**
   * Shortcut to kind attribute of lookahead token (la).
   */
  private Token.Kind sym;

  /**
   * Number of tokens scanned since last error.
   */
  private int errorDist;

  /**
   * According scanner
   */
  public final Scanner scanner;

  /**
   * According code buffer
   */
  public final Code code;

  /**
   * According symbol table
   */
  public final Tab tab;

  public Parser(Scanner scanner) {
    this.scanner = scanner;
    tab = new Tab(this);
    code = new Code(this);
    // Pseudo token to avoid crash when 1st symbol has scanner error.
    la = new Token(none, 1, 1);
    errorDist = MIN_ERROR_DIST;
  }

  /**
   * Reads ahead one symbol.
   */
  private void scan() {
    t = la;
    la = scanner.next();
    sym = la.kind;
    if (errorDist != Integer.MAX_VALUE) {
      errorDist++;
    }
  }

  /**
   * Verifies symbol and reads ahead.
   */
  private void check(Token.Kind expected) {
    if (sym == expected) {
      scan();
    } else {
      error(TOKEN_EXPECTED, expected);
    }
  }

  /**
   * Adds error message to the list of errors.
   */
  public void error(Message msg, Object... msgParams) {
    // TODO Exercise UE-P-3: Replace panic mode with error recovery (i.e., keep track of error distance)
    // TODO Exercise UE-P-3: Hint: Replacing panic mode also affects scan() method
    if (errorDist >= MIN_ERROR_DIST) {
      scanner.errors.error(la.line, la.col, msg, msgParams);
    }
    errorDist = 0;
  }

  /**
   * Starts the analysis.
   */
  public void parse() {
    scan(); // scan first symbol, initializes look-ahead
    Program(); // start analysis
    check(eof);
  }


  // ===============================================
  // TODO Exercise UE-P-2: Implementation of parser
  // TODO Exercise UE-P-3: Error recovery methods
  // TODO Exercise UE-P-4: Symbol table handling
  // TODO Exercise UE-P-5-6: Code generation
  // ===============================================

  // TODO Exercise UE-P-3: Error distance

  // TODO Exercise UE-P-2 + Exercise 3: Sets to handle certain first, follow, and recover sets

  static {
    // Initialize first and follow sets.

    firstStatement = new BitSet();
    firstStatement.set(ident.ordinal()); // First(Designator)
    firstStatement.set(if_.ordinal());
    firstStatement.set(while_.ordinal());
    firstStatement.set(break_.ordinal());
    firstStatement.set(return_.ordinal());
    firstStatement.set(read.ordinal());
    firstStatement.set(print.ordinal());
    firstStatement.set(lbrace.ordinal()); // First(Block)
    firstStatement.set(semicolon.ordinal());

    firstFactor = new BitSet();
    firstFactor.set(ident.ordinal()); // First(Designator)
    firstFactor.set(number.ordinal());
    firstFactor.set(charConst.ordinal());
    firstFactor.set(new_.ordinal());
    firstFactor.set(lpar.ordinal());

    firstAssignop = new BitSet();
    firstAssignop.set(assign.ordinal());
    firstAssignop.set(plusas.ordinal());
    firstAssignop.set(minusas.ordinal());
    firstAssignop.set(timesas.ordinal());
    firstAssignop.set(slashas.ordinal());
    firstAssignop.set(remas.ordinal());

    firstMulop = new BitSet();
    firstMulop.set(times.ordinal());
    firstMulop.set(slash.ordinal());
    firstMulop.set(rem.ordinal());

    firstRecoverGlobalDeclSet = EnumSet.of(final_, ident, class_);
    breakRecoverGlobalDeclSet = EnumSet.of(lbrace, eof);

    firstRecoverMethodDeclSet = EnumSet.of(ident, void_);
    breakRecoverMethodDeclSet = EnumSet.of(rbrace, eof);

    firstRecoverStatementSet = EnumSet.of(if_, while_, break_, return_, read, print, semicolon);
    breakRecoverStatementSet = EnumSet.of(rbrace, eof);
  }

  // ---------------------------------

  // TODO Exercise UE-P-2: One top-down parsing method per production

  /**
   * Program = <br>
   * "program" ident <br>
   * { ConstDecl | VarDecl | ClassDecl } <br>
   * "{" { MethodDecl } "}" .
   */
  private void Program() {
    // TODO Exercise UE-P-2
    check(program);
    check(ident);
    Obj prog = tab.insert(Obj.Kind.Prog, t.val, Tab.noType);
    tab.openScope();

    while (true) {
      if (firstRecoverGlobalDeclSet.contains(sym)) {
        switch (sym) {
          case final_ -> ConstDecl();
          case ident -> VarDecl(); // First(VarDecl) = First(Type)
          case class_-> ClassDecl();
          default -> error(TOKEN_EXPECTED, final_, ident, class_); // should not be reachable
        }
      } else if (breakRecoverGlobalDeclSet.contains(sym)) {
        break;
      } else {
        recoverDecl();
      }
    }
    if (tab.curScope.nVars() > MAX_GLOBALS) {
      error(TOO_MANY_GLOBALS);
    }

    check(lbrace);
    while (true) {
      if (firstRecoverMethodDeclSet.contains(sym)) {
        MethodDecl();
      } else if (breakRecoverMethodDeclSet.contains(sym)) {
        break;
      } else {
        recoverMethodDecl();
      }
    }
    check(rbrace);

    if (code.mainpc == -1) {
      error(MAIN_NOT_FOUND);
    }
    code.dataSize = tab.curScope.nVars();
    prog.locals = tab.curScope.locals();
    tab.closeScope();
  }

  // ...

  /**
   * ConstDecl = "final" Type ident "=" ( number | charConst ) ";".
   */
  private void ConstDecl() {
    check(final_);
    Struct type = Type();
    check(ident);
    Obj const_ = tab.insert(Obj.Kind.Con, t.val, type);

    check(assign);

    if ((sym == charConst && type != Tab.charType) ||
            (sym == number && type != Tab.intType)
    ) {
      error(INCOMPATIBLE_TYPES);
    }
    switch (sym) {
      case charConst, number -> {
        scan();
        const_.val = t.numVal;
      }
      default -> error(INVALID_CONST_TYPE);
    }

    check(semicolon);
  }

  /**
   * VarDecl = Type ident { "," ident } ";".
   */
  private void VarDecl() {
    Struct type = Type();
    check(ident);
    tab.insert(Obj.Kind.Var, t.val, type);

    while (sym == comma) {
      scan();
      check(ident);
      tab.insert(Obj.Kind.Var, t.val, type);
    }

    check(semicolon);
  }

  /**
   * ClassDecl = "class" ident "{" { VarDecl } "}".
   */
  private void ClassDecl() {
    check(class_);
    check(ident);
    Obj clazz = tab.insert(Obj.Kind.Type, t.val, new Struct(Struct.Kind.Class));

    check(lbrace);
    tab.openScope();
    while (sym == ident) {
      // First(VarDecl) = First(Type)
      VarDecl();
    }
    if (tab.curScope.nVars() > MAX_FIELDS) {
      error(TOO_MANY_FIELDS);
    }
    clazz.type.fields = tab.curScope.locals();
    check(rbrace);
    tab.closeScope();
  }

  /**
   * MethodDecl = ( Type | "void" ) ident "(" [ FormPars ] ")" { VarDecl } Block.
   */
  private void MethodDecl() {
    boolean isVoid = false;
    boolean hasPars = false;

    Struct type = Tab.noType;
    switch (sym) {
      case ident -> {
        type = Type();
        if (type.isRefType()) {
          error(ILLEGAL_METHOD_RETURN_TYPE);
        }
      }
      case void_ -> {
        scan();
        isVoid = true;
      }
      default -> error(INVALID_METHOD_DECL);
    }

    check(ident);
    String methodName = t.val;
    Obj meth = tab.insert(Obj.Kind.Meth, methodName, type);
    meth.adr = code.pc;

    check(lpar);
    tab.openScope();
    if (sym == ident) {
      // First(FormPars) = First(Type)
      meth.nPars = FormPars();
      hasPars = true;
    }
    check(rpar);
    if (methodName.equals("main")) {
      if (!isVoid) {
        error(MAIN_NOT_VOID);
      }
      if (hasPars) {
        error(MAIN_WITH_PARAMS);
      }
      code.mainpc = meth.adr;
    }
    while (sym == ident) {
      // First(VarDecl) = First(Type)
      VarDecl();
    }
    if (tab.curScope.nVars() > MAX_LOCALS) {
      error(TOO_MANY_LOCALS);
    }

    code.put(Code.OpCode.enter);
    code.put(meth.nPars);
    code.put(tab.curScope.nVars());
    Block(meth.type, null);
    if (isVoid) {
      code.put(Code.OpCode.exit);
      code.put(Code.OpCode.return_);
    } else { // non-void method reaches this point => it did not return on a branch => throw exception
      code.put(Code.OpCode.trap);
      code.put(1);
    }

    meth.locals = tab.curScope.locals();
    tab.closeScope();
  }

  /**
   * FormPars = Type ident { "," Type ident }.
   */
  private int FormPars() {
    int nPars = 0;

    Struct type = Type();
    check(ident);
    tab.insert(Obj.Kind.Var, t.val, type);
    nPars++;

    while (sym == comma) {
      scan();
      type = Type();
      check(ident);
      tab.insert(Obj.Kind.Var, t.val, type);
      nPars++;
    }

    return nPars;
  }

  /**
   * Type = ident [ "[" "]" ].
   */
  private Struct Type() {
    check(ident);
    Obj obj = tab.find(t.val);
    if (obj.kind != Obj.Kind.Type) {
      error(TYPE_EXPECTED);
    }

    Struct type = obj.type;
    if (sym == lbrack) {
      scan();
      check(rbrack);
      type = new Struct(type);
    }
    return type;
  }

  /**
   * Block = "{" { Statement } "}".
   */
  private void Block(Struct typeOfCurMeth, Label breakLabel) {
    check(lbrace);
    while (true) {
      if (firstStatement.get(sym.ordinal())) {
        Statement(typeOfCurMeth, breakLabel);
      } else if (sym == rbrace || sym == eof) {
        break;
      } else {
        recoverStat();
      }
    }
    check(rbrace);
  }

  /**
   * Statement = Designator ( Assignop Expr | ActPars | "++" | "--" ) ";"
   *    | "if" "(" Condition ")" Statement [ "else" Statement ]
   *    | "while" "(" Condition ")" Statement
   *    | "break" ";"
   *    | "return" [ Expr ] ";"
   *    | "read" "(" Designator ")" ";"
   *    | "print" "(" Expr [ "," number ] ")" ";"
   *    | Block
   *    | ";".
   */
  private void Statement(Struct typeOfCurMeth, Label breakLabel) {
    switch (sym) {
      case ident ->  {
        Operand x = Designator();
        if (firstAssignop.get(sym.ordinal())) {
          if (x.isReadOnly() && x.kind != Operand.Kind.Stack) {
            error(CANNOT_STORE_TO_READONLY, x.kind.name());
          } else {
            Code.OpCode operation = Assignop();
            if (operation != Code.OpCode.nop) {
              code.prepareLhsOfCompoundAssignment(x);
            }
            Operand y = Expr();
            if (!y.type.assignableTo(x.type)) {
              error(INCOMPATIBLE_TYPES);
            } else if (operation == Code.OpCode.nop) {
              // simple assignment
              code.assign(x, y);
            } else { // !x.isReadOnly() && operation != Code.OpCode.nop
              // compound assignment
              code.load(y);
              code.put(operation);
              Operand ignore = new Operand(0);
              ignore.kind = Operand.Kind.Stack;
              // ignore will be ignored because it is of kind Stack and the existing result will be used
              code.assign(x, ignore);
            }
          }
        } else if (sym == lpar) {
          ActPars(x);
          if (x.type != Tab.noType) {
            code.put(Code.OpCode.pop);
          }
        } else if (sym == pplus || sym == mminus) {
          if (x.type != Tab.intType) {
            error(INC_DEC_EXPECTS_INT);
          } else if (sym == pplus) {
            code.inc(x, 1);
          } else { // t.kind == mminus
            code.inc(x, -1);
          }
          scan();
        } else {
          error(INVALID_DESIGNATOR_STATEMENT);
        }
        check(semicolon);
      }
      case if_ -> {
        scan();

        check(lpar);
        Label end = new Label(code);
        Operand x = Condition();
        code.fJump(x.op, x.fLabel);
        x.tLabel.here();
        check(rpar);

        Statement(typeOfCurMeth, breakLabel);

        if (sym == else_) {
          code.jump(end);
          scan();
          x.fLabel.here();
          Statement(typeOfCurMeth, breakLabel);
        } else {
          x.fLabel.here();
        }
        end.here();
      }
      case while_ -> {
        scan();

        check(lpar);
        Label top = new Label(code);
        top.here();
        Operand x = Condition();
        code.fJump(x.op, x.fLabel);
        x.tLabel.here();
        check(rpar);

        Statement(typeOfCurMeth, x.fLabel);
        code.jump(top);
        x.fLabel.here();
      }
      case break_ -> {
        scan();
        if (breakLabel == null) {
          error(BREAK_OUTSIDE_LOOP);
        } else {
          code.jump(breakLabel);
        }
        check(semicolon);
      }
      case return_ -> {
        scan();
        if (sym == minus || firstFactor.get(sym.ordinal())) {
          // First(Expr) = minus, First(Term) = minus, First(Factor)
          Operand x = Expr();
          code.load(x);

          if (typeOfCurMeth == Tab.noType) {
            // Prepare error position requested by CodeGenerationTest.returnVoid() test
            Token tmp = la;
            la = t;
            error(UNEXPECTED_RETURN_VALUE);
            la = tmp;
          } else if (!x.type.assignableTo(typeOfCurMeth)) {
            error(RETURN_TYPE_MISMATCH);
          }
        } else if (typeOfCurMeth != Tab.noType) {
          error(MISSING_RETURN_VALUE);
        }

        code.put(Code.OpCode.exit);
        code.put(Code.OpCode.return_);

        check(semicolon);
      }
      case read -> {
        scan();
        check(lpar);
        Operand x = Designator();
        if (x.isReadOnly()) {
          error(CANNOT_STORE_TO_READONLY, x.kind.name());
        } else {
          if (x.type == Tab.charType) {
            code.put(Code.OpCode.bread);
          } else if (x.type == Tab.intType) {
            code.put(Code.OpCode.read);
          } else {
            error(ILLEGAL_READ_ARGUMENT);
          }
          Operand ignore = new Operand(0);
          ignore.kind = Operand.Kind.Stack;
          // ignore will be ignored because it is of kind Stack and the existing result will be used
          code.assign(x, ignore);
        }
        check(rpar);
        check(semicolon);
      }
      case print -> {
        scan();
        check(lpar);
        Operand x = Expr();
        code.load(x);
        int constant = 0;
        if (sym == comma) {
          scan();
          check(number);
          constant = t.numVal;
        }
        code.loadConst(constant);
        if (x.type == Tab.charType) {
          code.put(Code.OpCode.bprint);
        } else if (x.type == Tab.intType) {
          code.put(Code.OpCode.print);
        } else {
          error(ILLEGAL_PRINT_ARGUMENT);
        }
        check(rpar);
        check(semicolon);
      }
      case lbrace -> Block(typeOfCurMeth, breakLabel);
      case semicolon -> scan();
      default -> error(Message.INVALID_STATEMENT);
    }
  }

  /**
   * Assignop = "=" | "+=" | "-=" | "*=" | "/=" | "%=".
   */
  private Code.OpCode Assignop() {
    switch (sym) {
      case assign -> {
        scan();
        return Code.OpCode.nop;
      }
      case plusas -> {
        scan();
        return Code.OpCode.add;
      }
      case minusas -> {
        scan();
        return Code.OpCode.sub;
      }
      case timesas -> {
        scan();
        return Code.OpCode.mul;
      }
      case slashas -> {
        scan();
        return Code.OpCode.div;
      }
      case remas -> {
        scan();
        return Code.OpCode.rem;
      }
      default -> {
        error(INVALID_ASSIGN_OP);
        return Code.OpCode.nop;
      }
    }
  }

  /**
   * ActPars = "(" [ Expr { "," Expr } ] ")".
   */
  private void ActPars(Operand meth) {
    check(lpar);
    
    if (meth.kind != Operand.Kind.Meth) {
      error(CALL_TO_NON_METHOD);
      meth.obj = tab.noObj;
      return;
    }
    int aPars = 0;
    int fPars = meth.obj.nPars;
    List<Obj> paramList = meth.obj.locals.values().stream().toList();
    
    if (sym == minus || firstFactor.get(sym.ordinal())) {
      // First(Expr) = minus, First(Term) = minus, First(Factor)
      Operand x = Expr();
      code.load(x);
      Obj fp = aPars < paramList.size() ? paramList.get(aPars) : null;
      aPars++;
      if (fp != null && !x.type.assignableTo(fp.type)) {
        error(ARGUMENT_TYPE_MISMATCH);
      }
      while (sym == comma) {
        scan();
        x = Expr();
        code.load(x);
        fp = aPars < paramList.size() ? paramList.get(aPars) : null;
        aPars++;
        if (fp != null && !x.type.assignableTo(fp.type)) {
          error(ARGUMENT_TYPE_MISMATCH);
        }
      }
    }

    if (aPars != fPars) {
      error(WRONG_ARGUMENT_COUNT);
    }
    
    check(rpar);

    code.methodCall(meth);
    meth.kind = Operand.Kind.Stack;
  }

  /**
   * Condition = CondTerm { "||" CondTerm }.
   */
  private Operand Condition() {
    Operand x = CondTerm();
    while (sym == or) {
      code.tJump(x.op, x.tLabel);
      scan();
      x.fLabel.here();
      Operand y = CondTerm();
      x.fLabel = y.fLabel;
      x.op = y.op;
    }
    return x;
  }

  /**
   * CondTerm = CondFact { "&&" CondFact }.
   */
  private Operand CondTerm() {
    Operand x = CondFact();
    while (sym == and) {
      code.fJump(x.op, x.fLabel);
      scan();
      Operand y = CondFact();
      x.op = y.op;
    }
    return x;
  }

  /**
   * CondFact = Expr Relop Expr.
   */
  private Operand CondFact() {
    Operand x = Expr();
    code.load(x);
    Code.CompOp op = Relop();
    Operand y = Expr();
    code.load(y);
    if (!x.type.compatibleWith(y.type)) {
      error(INCOMPATIBLE_TYPES);
    }
    if (x.type.isRefType() && op != Code.CompOp.eq && op != Code.CompOp.ne) {
      error(ILLEGAL_REFERENCE_COMPARISON);
    }
    return new Operand(op, code);
  }

  /**
   * Relop = "==" | "!=" | ">" | ">=" | "<" | "<=".
   */
  private Code.CompOp Relop() {
    switch (sym) {
      case eql -> {
        scan();
        return Code.CompOp.eq;
      }
      case neq -> {
        scan();
        return Code.CompOp.ne;
      }
      case gtr -> {
        scan();
        return Code.CompOp.gt;
      }
      case geq -> {
        scan();
        return Code.CompOp.ge;
      }
      case lss -> {
        scan();
        return Code.CompOp.lt;
      }
      case leq -> {
        scan();
        return Code.CompOp.le;
      }
      default -> {
        error(INVALID_REL_OP);
        return Code.CompOp.eq;
      }
    }
  }

  /**
   * Expr = [ "–" ] Term { Addop Term }.
   */
  private Operand Expr() {
    boolean hasUnaryMinus = false;
    if (sym == minus) {
      scan();
      hasUnaryMinus = true;
    }

    Operand x = Term();

    if (hasUnaryMinus) {
      if (x.type == Tab.intType) {
        if (x.kind == Operand.Kind.Con) { x.val = - x.val; }
        else {
          code.load(x);
          code.put(Code.OpCode.neg);
          x = new Operand(Tab.intType);
          x.kind = Operand.Kind.Stack;
        }
      } else {
        error(UNARY_MINUS_EXPECTS_INT);
        return new Operand(1);
      }
    }

    while (sym == plus || sym == minus) {
      Code.OpCode addopOpCode = Addop();

      code.load(x);

      Operand y = Term();
      if (areCompatibleAndInt(x, y)) {
        error(INCOMPATIBLE_TYPES);
        return new Operand(1);
      }

      code.load(y);
      code.put(addopOpCode);
    }

    return x;
  }

  /**
   * Term = Factor { Mulop Factor }.
   */
  private Operand Term() {
    Operand x = Factor();
    while (firstMulop.get(sym.ordinal())) {
      Code.OpCode mulopOpCode = Mulop();

      code.load(x);

      Operand y = Factor();
      if (areCompatibleAndInt(x, y)) {
        error(INCOMPATIBLE_TYPES);
        return new Operand(1);
      }

      code.load(y);
      code.put(mulopOpCode);
    }
    return x;
  }

  /**
   * Factor = Designator [ ActPars ]
   *    | number
   *    | charConst
   *    | "new" ident [ "[" Expr "]" ]
   *    | "(" Expr ")".
   */
  private Operand Factor() {
    Operand x;
    switch (sym) {
      case ident -> {
        x = Designator();
        if (sym == lpar) {
          if (x.type == Tab.noType) {
            error(VOID_CALL_IN_EXPRESSION);
            return new Operand(1);
          }

          ActPars(x);

          x.val = code.buf[code.pc];
        }
      }
      case number -> {
        scan();
        x = new Operand(t.numVal);
      }
      case charConst -> {
        scan();
        x = new Operand(Tab.charType);
        x.kind = Operand.Kind.Con;
        x.val = t.numVal;
      }
      case new_ -> {
        scan();

        check(ident);
        Obj obj = tab.find(t.val);
        if (obj.kind != Obj.Kind.Type) {
          error(TYPE_EXPECTED);
          return new Operand(1);
        }

        if (sym == lbrack) {
          scan();
          x = new Operand(new Struct(obj.type));
          Operand y = Expr();
          if (y.type != Tab.intType) {
            error(ARRAY_SIZE_EXPECTS_INT);
          } else {
            code.load(y);
            code.put(Code.OpCode.newarray);
            if (obj.type == Tab.charType) {
              code.put(0);
            } else {
              code.put(1);
            }
          }
          check(rbrack);
        } else {
          x = new Operand(obj.type);
          if (x.type.kind != Struct.Kind.Class) {
            error(CLASS_TYPE_EXPECTED);
            return new Operand(1);
          }
          code.put(Code.OpCode.new_);
          code.put2(x.type.fields.size());
        }
      }
      case lpar -> {
        scan();
        x = Expr();
        check(rpar);
      }
      default -> {
        error(INVALID_FACTOR);
        x = new Operand(0);
      }
    }

    return x;
  }

  /**
   * Designator = ident { "." ident | "[" [ "~" ] Expr "]" }.
   */
  private Operand Designator() {
    check(ident);
    Operand x = new Operand(tab.find(t.val), this);
    Operand.Kind kindBeforeLoad = x.kind;

    while (sym == period || sym == lbrack) {
      switch (sym) {
        case period -> {
          if (x.type.kind != Struct.Kind.Class) {
            error(FIELD_ACCESS_TO_NON_CLASS);
          }
          scan();
          check(ident);
          code.load(x);
          Obj obj = tab.findField(t.val, x.type);
          x.type = obj.type;
          x.adr = obj.adr;
          x.kind = Operand.Kind.Fld;
          kindBeforeLoad = x.kind;
        }
        case lbrack -> {
          if (x.type.kind != Struct.Kind.Arr) {
            error(INDEXED_ACCESS_TO_NON_ARRAY);
          }

          scan();

          boolean isTildeDetected = false;
          if (sym == tilde) {
            scan();
            isTildeDetected = true;
          }

          code.load(x);
          if (x.type.kind != Struct.Kind.Arr) {
            x.kind = Operand.Kind.None;
            kindBeforeLoad = x.kind;
            x.type = Tab.noType;
          } else {
            x.kind = Operand.Kind.Elem;
            kindBeforeLoad = x.kind;
            x.type = x.type.elemType;
          }

          if (isTildeDetected) {
            code.put(Code.OpCode.dup);
            code.put(Code.OpCode.arraylength);
          }

          Operand y = Expr();

          if (y.type.kind != Struct.Kind.Int) {
            error(ARRAY_INDEX_EXPECTS_INT);
            code.loadConst(1);
          } else {
            code.load(y);
            if (isTildeDetected) {
              code.put(Code.OpCode.sub);
            }
          }
          check(rbrack);
        }
        default -> error(TOKEN_EXPECTED, period, lbrack);
      }
    }

    x.kind = kindBeforeLoad;
    return x;
  }

  /**
   * Addop = "+" | "–".
   */
  private Code.OpCode Addop() {
    switch (sym) {
      case plus -> {
        scan();
        return Code.OpCode.add;
      }
      case minus -> {
        scan();
        return Code.OpCode.sub;
      }
      default -> {
        error(INVALID_ADD_OP);
        return Code.OpCode.nop;
      }
    }
  }

  /**
   * Mulop = "*" | "/" | "%".
   */
  private Code.OpCode Mulop() {
    switch (sym) {
      case times -> {
        scan();
        return Code.OpCode.mul;
      }
      case slash -> {
        scan();
        return Code.OpCode.div;
      }
      case rem -> {
        scan();
        return Code.OpCode.rem;
      }
      default -> {
        error(INVALID_MUL_OP);
        return Code.OpCode.nop;
      }
    }
  }

  // ------------------------------------

  // TODO Exercise UE-P-3: Error recovery methods: recoverDecl, recoverMethodDecl and recoverStat (+ TODO Exercise UE-P-5: Check idents for Type kind)

  private void recoverDecl() {
    error(DECLARATION_RECOVERY);

    do {
      scan();
    } while (!firstRecoverGlobalDeclSet.contains(sym) && !breakRecoverGlobalDeclSet.contains(sym));
    errorDist = 0;
  }

  private void recoverMethodDecl() {
    error(METHOD_DECL_RECOVERY);

    do {
      scan();
    } while (!firstRecoverMethodDeclSet.contains(sym) && !breakRecoverMethodDeclSet.contains(sym));
    errorDist = 0;
  }

  private void recoverStat() {
    if (!firstRecoverStatementSet.contains(sym) && !breakRecoverStatementSet.contains(sym)) {
      error(STATEMENT_RECOVERY);
    }

    do {
      scan();
    } while (!firstRecoverStatementSet.contains(sym) && !breakRecoverStatementSet.contains(sym));
    errorDist = 0;
  }

  // ====================================
  // ====================================

  private boolean areCompatibleAndInt(Operand x, Operand y) {
    return !x.type.compatibleWith(y.type) && x.type == Tab.intType;
  }
}
