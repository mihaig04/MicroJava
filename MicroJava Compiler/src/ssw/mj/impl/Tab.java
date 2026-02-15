package ssw.mj.impl;

import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Scope;
import ssw.mj.symtab.Struct;

import static ssw.mj.Errors.Message.DUPLICATE_NAME_IN_SCOPE;
import static ssw.mj.Errors.Message.NAME_NOT_FOUND;
import static ssw.mj.Errors.Message.FIELD_NOT_FOUND;

public final class Tab {

  // Universe
  public static final Struct noType = new Struct(Struct.Kind.None);
  public static final Struct intType = new Struct(Struct.Kind.Int);
  public static final Struct charType = new Struct(Struct.Kind.Char);
  public static final Struct nullType = new Struct(Struct.Kind.Class);

  public final Obj noObj;
  public final Obj chrObj;
  public final Obj ordObj;
  public final Obj lenObj;

  /**
   * Only used for reporting errors.
   */
  private final Parser parser;

  /**
   * The current top scope.
   */
  public Scope curScope = null;

  // First scope opening (universe) will increase this to -1
  /**
   * Nesting level of current scope.
   */
  private int curLevel = -2;

  public Tab(Parser p) {
    parser = p;

    // setting up "universe" (= predefined names)
    // opening scope (curLevel goes to -1, which is the universe level)
    openScope();

    noObj = new Obj(Obj.Kind.Var, "noObj", noType);

    insert(Obj.Kind.Type, "int", intType);
    insert(Obj.Kind.Type, "char", charType);
    insert(Obj.Kind.Con, "null", nullType);

    chrObj = insert(Obj.Kind.Meth, "chr", charType);
    openScope();
    Obj iVarObj = insert(Obj.Kind.Var, "i", intType);
    iVarObj.level = 1;
    chrObj.nPars = curScope.nVars();
    chrObj.locals = curScope.locals();
    closeScope();

    // TODO Exercise UE-P-4: build "ord" universe method and store in ordObj
    ordObj = insert(Obj.Kind.Meth, "ord", intType);
    openScope();
    Obj chVarObj = insert(Obj.Kind.Var, "ch", charType);
    chVarObj.level = 1;
    ordObj.nPars = curScope.nVars();
    ordObj.locals = curScope.locals();
    closeScope();

    // TODO Exercise UE-P-4: build "len" universe method and store in lenObj
    lenObj = insert(Obj.Kind.Meth, "len", intType);
    openScope();
    Obj arrObj = insert(Obj.Kind.Var, "arr", new Struct(noType));
    arrObj.level = 1;
    lenObj.nPars = curScope.nVars();
    lenObj.locals = curScope.locals();
    closeScope();

    // still on level -1
    // now that the universe is constructed, the next node that will be added is the Program itself
    // (which will open its own scope with level 0)
  }

  // ===============================================
  // TODO Exercise UE-P-4: implementation of symbol table
  // ===============================================

  public void openScope() {
    curScope = new Scope(curScope);
    curLevel++;
  }

  public void closeScope() {
    curScope = curScope.outer();
    curLevel--;
  }

  public Obj insert(Obj.Kind kind, String name, Struct type) {
    // TODO Exercise UE-P-4
    Obj obj = new Obj(kind, name, type);

    if (curScope.findLocal(name) != null) {
      parser.error(DUPLICATE_NAME_IN_SCOPE, name);
    }

    if (kind == Obj.Kind.Var) {
      obj.adr = curScope.nVars();
      obj.level = curLevel;
    }

    curScope.insert(obj);
    return obj;
  }

  /**
   * Retrieves the object with <code>name</code> from the innermost scope.
   */
  public Obj find(String name) {
    // TODO Exercise UE-P-4
    Obj obj = curScope.findGlobal(name);
    if (obj == null) {
      parser.error(NAME_NOT_FOUND, name);
      return noObj;
    }
    return obj;
  }

  /**
   * Retrieves the field <code>name</code> from the fields of
   * <code>type</code>.
   */
  public Obj findField(String name, Struct type) {
    // TODO Exercise UE-P-4
    Obj obj = type.findField(name);
    if (obj == null) {
      parser.error(FIELD_NOT_FOUND, name);
      return noObj;
    }
    return obj;
  }

  // ===============================================
  // ===============================================
}
