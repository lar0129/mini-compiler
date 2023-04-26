import org.antlr.v4.runtime.ParserRuleContext;
import scope.*;
import symbol.*;
import type.*;

public class SymbolDetectVisitor extends SysYParserBaseVisitor<Void>{
    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private int localScopeCounter = 0;

    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
        // 进入新的 Scope
        globalScope = new GlobalScope(null);
        currentScope = globalScope;
        // 遍历子树
        Void ret = super.visitProgram(ctx);
        // 回到上一层 Scope
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        // 报告 Error type 4 函数重复定义

        // 进入新的 Scope，定义新的 Symbol
        String typeName = ctx.funcType().getText();
        globalScope.resolve(typeName);
        String funName = ctx.IDENT().getText();
        FunctionSymbol fun = new FunctionSymbol(funName, currentScope);
        // 是scope也是symbol,需要放到符号表里
        currentScope.define(fun);
        currentScope = fun;

        // 遍历子树
        Void ret = super.visitFuncDef(ctx);
        // 回到上一层 Scope
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        // 进入新的 Scope
        LocalScope localScope = new LocalScope(currentScope);
        String localScopeName = localScope.getName() + localScopeCounter;
        localScope.setName(localScopeName);
        localScopeCounter++;
        currentScope = localScope;

        // 遍历子树
        Void ret = super.visitBlock(ctx);
        // 回到上一层 Scope
        return ret;
    }

    int getLineNo(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            // 报告 Error type 3 变量重复声明
            if (varDefContext.ASSIGN() != null) {
                // 报告 Error type 5 赋值号两侧类型不匹配
            }
            // 定义新的 Symbol
            String typeName = ctx.bType().getText();
            Type type = (Type) globalScope.resolve(typeName);
            String varName = varDefContext.getText();
            VariableSymbol varSymbol = new VariableSymbol(varName, type);
            currentScope.define(varSymbol);
        }

        return super.visitVarDecl(ctx);
    }

    @Override
    public Void visitConstDecl(SysYParser.ConstDeclContext ctx) {
        // 结构同 visitVarDecl
        for (SysYParser.ConstDefContext constDefContext : ctx.constDef()) {
            // 报告 Error type 3 变量重复声明
            if (constDefContext.ASSIGN() != null) {
                // 报告 Error type 5 赋值号两侧类型不匹配
            }
            // 定义新的 Symbol
            String typeName = ctx.bType().getText();
            Type type = (Type) globalScope.resolve(typeName);
            String varName = constDefContext.getText();
            VariableSymbol varSymbol = new VariableSymbol(varName, type);
            currentScope.define(varSymbol);
        }
        return super.visitConstDecl(ctx);
    }

    @Override
    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        // 报告 Error type 3 变量重复声明

        // 定义新的 Symbol
        String typeName = ctx.bType().getText();
        Type type = (Type) globalScope.resolve(typeName);
        String varName = ctx.IDENT().getText();
        VariableSymbol varSymbol = new VariableSymbol(varName, type);
        currentScope.define(varSymbol);

        return super.visitFuncFParam(ctx);
    }

    private Type getLValType(SysYParser.LValContext ctx) {
        // 通过符号表获取？
        Type varType = (Type) ctx.IDENT();
        return varType;
    }


    @Override
    public Void visitLVal(SysYParser.LValContext ctx) {
        // 报告 Error type 1 变量未声明
        int arrayDimension = 0;
        for (int i = 0; i < arrayDimension; ++i) {
            // 报告 Error type 9 对非数组使用下标运算符
        }

        return super.visitLVal(ctx);
    }

    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.ASSIGN() != null) {
            // 报告 Error type 11 对函数进行赋值操作
            // 报告 Error type 5 赋值号两侧类型不匹配
        } else if (ctx.RETURN() != null) {
            // 报告 Error type 7 返回值类型不匹配
        }
        return super.visitStmt(ctx);
    }

    private Type getExpType(SysYParser.ExpContext ctx) {
//      也可以在g4中直接标记？
        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN
        } else if (ctx.L_PAREN() != null) { // L_PAREN exp R_PAREN
        } else if (ctx.unaryOp() != null) { // unaryOp exp
        } else if (ctx.lVal() != null) { // lVal
        } else if (ctx.number() != null) { // number
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
        }
        return new BasicTypeSymbol("noType");
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN
            // 报告 Error type 2 函数未定义
            // 报告 Error type 10 对变量使用函数调用
            // 报告 Error type 8 函数参数不适用
        } else if (ctx.unaryOp() != null) { // unaryOp exp
            // 报告 Error type 6 运算符需求类型与提供类型不匹配
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            // 报告 Error type 6 运算符需求类型与提供类型不匹配
        }
        return super.visitExp(ctx);
    }

    @Override
    public Void visitCond(SysYParser.CondContext ctx) {
        // 报告 Error type 6 运算符需求类型与提供类型不匹配
        return super.visitCond(ctx);
    }


    public boolean getErrorFound() {
        return false;
    }
}

