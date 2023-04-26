import scope.*;
import symbol.*;
import type.*;

import java.util.List;

public class SymbolTableListener extends SysYParserBaseListener{

    private GlobalScope globalScope = null;
    private Scope currentScope = null;

    private int localScopeCounter = 0;

    /**
     * (1) When/How to start/enter a new scope?
     */
    @Override
    public void enterProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        currentScope = globalScope;
    }

    @Override
    public void enterFuncDef(SysYParser.FuncDefContext ctx) {

        String typeName = ctx.funcType().getText();
        globalScope.resolve(typeName);

        String funName = ctx.IDENT().getText();
        FunctionSymbol fun = new FunctionSymbol(funName, currentScope);

        // 是scope也是symbol,需要放到符号表里
        currentScope.define(fun);
        currentScope = fun;
    }

    @Override
    public void enterBlock(SysYParser.BlockContext ctx){
        LocalScope localScope = new LocalScope(currentScope);
        String localScopeName = localScope.getName() + localScopeCounter;
        localScope.setName(localScopeName);
        localScopeCounter++;

        currentScope = localScope;
    }
    /**
     * (2) When/How to exit the current scope?
     */
    @Override
    public void exitProgram(SysYParser.ProgramContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void exitFuncDef(SysYParser.FuncDefContext ctx) {
           currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void exitBlock(SysYParser.BlockContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    /**
     * (3) When to define symbols?
     */

    @Override
    public void exitVarDecl(SysYParser.VarDeclContext ctx)  {
        String typeName = ctx.bType().getText();
        Type type = (Type) globalScope.resolve(typeName);
        List<SysYParser.VarDefContext> varLists = ctx.varDef();
        for(SysYParser.VarDefContext var:varLists) {
            String varName = var.getText();
            VariableSymbol varSymbol = new VariableSymbol(varName, type);
            currentScope.define(varSymbol);
        }
    }

    // 定义单个函数
    @Override
    public void enterFuncFParam(SysYParser.FuncFParamContext ctx){
        String typeName = ctx.bType().getText();
        Type type = (Type) globalScope.resolve(typeName);

        String varName = ctx.IDENT().getText();
        VariableSymbol varSymbol = new VariableSymbol(varName, type);

        currentScope.define(varSymbol);
    }

    /**
     * (4) When to resolve symbols?
     */
//    @Override
//    public void exitId(CymbolParser.IdContext ctx) {
//        String varName = ctx.ID().getText();
//        currentScope.resolve(varName);
//    }

}
