import org.antlr.v4.runtime.ParserRuleContext;
import scope.*;
import symbol.*;
import type.*;

import java.util.ArrayList;
import java.util.Map;

public class SymbolDetectVisitor extends SysYParserBaseVisitor<Void>{
    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private int localScopeCounter = 0;

    private SymbolErrorTable errorTable = new SymbolErrorTable();

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
        String typeName = ctx.funcType().getText();
        String funName = ctx.IDENT().getText();
        Symbol funcSymbolInTable = globalScope.resolve(funName);

        // 报告 Error type 4 函数重复定义
        if(funcSymbolInTable != null){
            errorTable.addErrorTable(getLineNo(ctx),4);
            return null;
//            funName = funName + " fix:"+getLineNo(ctx);
        }

        // 修复错误，进入新的 Scope，定义新的 Symbol
        FunctionSymbol fun = new FunctionSymbol(funName, currentScope);
        fun.setFunctionType((Type) globalScope.resolve(typeName),new ArrayList<>());// 具体参数待进入FParam再填入

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
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    int getLineNo(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            String varName = varDefContext.IDENT().getText();
            Symbol varNameInTable = currentScope.resolveInScope(varName);
            // 报告 Error type 3 变量重复声明
            if(varNameInTable != null){
                errorTable.addErrorTable(getLineNo(ctx),3);
                continue;
            }

            String typeName = ctx.bType().getText();
            Type type = (Type) globalScope.resolve(typeName);
            int arrayDimension = varDefContext.constExp().size();
            while(arrayDimension!=0){
                ArrayType tempArrayType = new ArrayType();
                tempArrayType.setElementType(type);
                if(varDefContext.constExp(arrayDimension-1).exp().number()!=null) {
                    tempArrayType.setElementNums(Integer.parseInt(varDefContext.constExp(arrayDimension - 1).exp().number().getText()));
                    if(type instanceof BasicTypeSymbol)
                        tempArrayType.setArrayDimension(1);
                    else if(type instanceof ArrayType)
                        tempArrayType.setArrayDimension(((ArrayType)type).arrayDimension+1);
                }
                type = tempArrayType;
                arrayDimension--;
            }

            if (varDefContext.ASSIGN() != null) {
                // 报告 Error type 5 赋值号两侧类型不匹配
                String Ltype = type.toString();
                String Rtype = getInitValType(varDefContext.initVal()).toString();
//                System.out.println(getLineNo(ctx) + " Ltype: " + Ltype + ", Rtype: " + Rtype);
                if(!Ltype.equals(Rtype) &&
                        !(Ltype.equals("no type") || Rtype.equals("no type") )){
                    errorTable.addErrorTable(getLineNo(ctx),5);
                    continue;
                }
            }

//             定义新的 Symbol
            printType(type);
            VariableSymbol varSymbol = new VariableSymbol(varName, type);
            currentScope.define(varSymbol);
        }

        return super.visitVarDecl(ctx);
    }

    @Override
    public Void visitConstDecl(SysYParser.ConstDeclContext ctx) {
        // 结构同 visitVarDecl
        for (SysYParser.ConstDefContext constDefContext : ctx.constDef()) {

            String constName = constDefContext.IDENT().getText();
            Symbol constNameInTable = currentScope.resolveInScope(constName);
            // 报告 Error type 3 变量重复声明
            if(constNameInTable != null){
                errorTable.addErrorTable(getLineNo(ctx),3);
                continue;
            }

            String typeName = ctx.bType().getText();
            Type type = (Type) globalScope.resolve(typeName);
            int arrayDimension = constDefContext.constExp().size();
            while(arrayDimension!=0){
                ArrayType tempArrayType = new ArrayType();
                tempArrayType.setElementType(type);
                if(constDefContext.constExp(arrayDimension-1).exp().number()!=null) {
                    tempArrayType.setElementNums(Integer.parseInt(constDefContext.constExp(arrayDimension - 1).exp().number().getText()));
                    if(type instanceof BasicTypeSymbol)
                        tempArrayType.setArrayDimension(1);
                    else if(type instanceof ArrayType)
                        tempArrayType.setArrayDimension(((ArrayType)type).arrayDimension+1);
                }
                type = tempArrayType;
                arrayDimension--;
            }

            if (constDefContext.ASSIGN() != null) {
                // 报告 Error type 5 赋值号两侧类型不匹配
                String Ltype = type.toString();
                String Rtype = getConstInitValType(constDefContext.constInitVal()).toString();
                if(!Ltype.equals(Rtype) &&
                        !(Ltype.equals("no type") || Rtype.equals("no type") ))
                {
                    errorTable.addErrorTable(getLineNo(ctx),5);
                    continue;
                }
            }

            // 定义新的 Symbol
            printType(type);

            VariableSymbol varSymbol = new VariableSymbol(constName, type);
            currentScope.define(varSymbol);
        }
        return super.visitConstDecl(ctx);
    }

    @Override
    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {

        String varName = ctx.IDENT().getText();
        Symbol varNameInTable = currentScope.resolveInScope(varName);
        // 报告 Error type 3 变量重复声明
        if(varNameInTable != null){
            errorTable.addErrorTable(getLineNo(ctx),3);
            return null;
        }

        // 定义新的 Symbol
        String typeName = ctx.bType().getText();
        Type type = (Type) globalScope.resolve(typeName);
        int arrayDimension = ctx.L_BRACKT().size();

        // 处理定长数组
        while(arrayDimension>1){
            ArrayType tempArrayType = new ArrayType();
            tempArrayType.setElementType(type);
            if(ctx.exp(arrayDimension-2).number()!=null) {
                tempArrayType.setElementNums(Integer.parseInt(ctx.exp(arrayDimension - 2).number().getText()));
                tempArrayType.setArrayDimension(tempArrayType.arrayDimension++);
            }
            type = tempArrayType;
            arrayDimension--;
        }
        // 处理不定长数组
        if(arrayDimension == 1) {
            if(type instanceof ArrayType){
                type = new ArrayType(type, -1, ((ArrayType) type).arrayDimension + 1);
            }
            else if(type instanceof  BasicTypeSymbol){
                type = new ArrayType(type, -1, 1);
            }
        }

        printType(type);
        // 存入scope
        VariableSymbol varSymbol = new VariableSymbol(varName, type);
        currentScope.define(varSymbol);
        // 存入funcType
        Symbol funcSymbol = globalScope.resolve(currentScope.getName());
        if(funcSymbol instanceof FunctionSymbol){
            FunctionType funcType = ((FunctionSymbol) funcSymbol).getType();
            funcType.addParams(type);
        }

        return super.visitFuncFParam(ctx);
    }

    private void printType(Type type){
        //            打印数组类型
//            if(type instanceof BasicTypeSymbol){
//                System.out.println("BasicTypeSymbol : " + type);
//            }
//            if(type instanceof ArrayType) {
//                System.out.print("ArrayType : ");
//                Type type1 = type;
//                while (type1 instanceof ArrayType){
//                    System.out.print(((ArrayType) type1).elementNums + " ");
//                    type1 = ((ArrayType) type1).elementType;
//                }
//                System.out.println(type1);
//            }
    }

    @Override
    public Void visitLVal(SysYParser.LValContext ctx) {
        // 报告 Error type 1 变量未声明
        String varName = ctx.IDENT().getText();
        Symbol varNameInTable = currentScope.resolve(varName);
        if(varNameInTable == null){
//            System.out.println(varNameInTable.getName());
            errorTable.addErrorTable(getLineNo(ctx),1);
            return null;
        }

        // 报告 Error type 9 对非数组使用下标运算符
        if(varNameInTable instanceof FunctionSymbol){
            if(ctx.exp().size() > 0) {
                errorTable.addErrorTable(getLineNo(ctx), 9);
                return null;
            }
        }
        else if(varNameInTable instanceof VariableSymbol) {
            // 检查数组内部（包含0维）
            Type varType = ((VariableSymbol) varNameInTable).getType();
            int getInDimension = ctx.exp().size();
            for (int i = 0; i < getInDimension; ++i) {
                // 报告 Error type 9 对非数组使用下标运算符
                if( !(varType instanceof ArrayType) ){
                    errorTable.addErrorTable(getLineNo(ctx),9);
                    return null;
                }
                varType = ((ArrayType) varType).elementType;
            }
        }
        return super.visitLVal(ctx);
    }

    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.ASSIGN() != null) {
            // 报告 Error type 11 对函数进行赋值操作
            String lValName = ctx.lVal().IDENT().getText();
            Symbol lValInTable = currentScope.resolve(lValName);
            if (lValInTable instanceof FunctionSymbol){
                errorTable.addErrorTable(getLineNo(ctx),11);
                return null;
            }

            // 报告 Error type 5 赋值号两侧类型不匹配
            String Ltype = getLValType(ctx.lVal()).toString();
            String Rtype = getExpType(ctx.exp()).toString();
//            System.out.println(getLineNo(ctx) + " Ltype: " + Ltype + ", Rtype: " + Rtype);
            if(!Ltype.equals(Rtype) &&
                    !(Ltype.equals("no type") || Rtype.equals("no type") ) )
            {
                errorTable.addErrorTable(getLineNo(ctx),5);
                return null;
            }
        } else if (ctx.RETURN() != null) {
            // 报告 Error type 7 返回值类型不匹配
            String returnType = "";
            String funcRetType = "can't find";
            if(ctx.exp() != null)
                returnType = getExpType(ctx.exp()).toString();
            else
                returnType = "void";

            Symbol funcSymbol = globalScope.resolve(currentScope.getEnclosingScope().getName());
            if(funcSymbol instanceof FunctionSymbol){
                FunctionType funcType = ((FunctionSymbol) funcSymbol).getType();
                funcRetType = funcType.getRetTy().toString();
            }
//            System.out.println("ret: " +returnType + " funcRetType: " + funcRetType);
            if(returnType != funcRetType){
                errorTable.addErrorTable(getLineNo(ctx),7);
                return null;
            }
        }
        return super.visitStmt(ctx);
    }

    private Type getLValType(SysYParser.LValContext ctx) {
        Symbol symbol =  currentScope.resolve(ctx.IDENT().getText());
        if(symbol == null){
            return new BasicTypeSymbol("no type");
        }
        if(symbol instanceof FunctionSymbol) {
            return ((FunctionSymbol)symbol).getType();
        }
        else {
            Type tempType = ((VariableSymbol)symbol).getType();
            if(tempType instanceof ArrayType){
                tempType = ((ArrayType) tempType).clone();
                int arrayDeep = ctx.exp().size();
                while (arrayDeep>0) {
                    if (!(tempType instanceof ArrayType)){
                        tempType = new BasicTypeSymbol("Error_Array");
                        return tempType;
                    }
                    tempType = ((ArrayType)tempType).elementType;
                    arrayDeep--;
//                    ((ArrayType) tempType).setArrayDimension(((ArrayType) tempType).arrayDimension - arrayDeep);
                }
            }
            return tempType;
        }
    }

    private Type getExpType(SysYParser.ExpContext ctx) {
//      也可以在g4中直接标记？
        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN
//            System.out.println(ctx.IDENT().getText());
            Symbol symbol =  currentScope.resolve(ctx.IDENT().getText());
            if(symbol instanceof FunctionSymbol)
                return ((FunctionSymbol) symbol).getType().getRetTy();
            else {
                return new BasicTypeSymbol("no type");
            }
        } else if (ctx.L_PAREN() != null) { // L_PAREN exp R_PAREN
            return getExpType(ctx.exp(0));
        } else if (ctx.unaryOp() != null) { // unaryOp exp
            return getExpType(ctx.exp(0));
        } else if (ctx.lVal() != null) { // lVal
            return getLValType(ctx.lVal());
        } else if (ctx.number() != null) { // number
            return new BasicTypeSymbol("int");
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null ||
                ctx.PLUS() != null || ctx.MINUS() != null) {
            return new BasicTypeSymbol("int");
        }
        return new BasicTypeSymbol("no type");
    }


    private Type getInitValType(SysYParser.InitValContext ctx) {
        if(ctx.L_BRACE()!=null) {
            if(ctx.initVal().size()==0){
                return new ArrayType(new BasicTypeSymbol("int"),0,1);
            }
            else {
                Type sonInitValType = getInitValType(ctx.initVal(0));
                ArrayType arrayType = null;
                if(sonInitValType instanceof ArrayType){
                    arrayType = new ArrayType(sonInitValType,1,((ArrayType)sonInitValType).arrayDimension+1);
                }
                else {
                    arrayType = new ArrayType(new BasicTypeSymbol("int"),0,1);
                }
                return arrayType;
            }
        }
        else {
            return getExpType(ctx.exp());
        }
    }

    private Type getConstInitValType(SysYParser.ConstInitValContext ctx) {
        if(ctx.L_BRACE()!=null) {
            if(ctx.constInitVal().size()==0){
                return new ArrayType(new BasicTypeSymbol("int"),0,1);
            }
            else {
                Type sonInitValType = getConstInitValType(ctx.constInitVal(0));
                ArrayType arrayType = null;
                if(sonInitValType instanceof ArrayType){
                    arrayType = new ArrayType(sonInitValType,1,((ArrayType)sonInitValType).arrayDimension+1);
                }
                else {
                    arrayType = new ArrayType(new BasicTypeSymbol("int"),0,1);
                }
                return arrayType;
            }
        }
        else {
            return getExpType(ctx.constExp().exp());
        }
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN
            // 报告 Error type 2 函数未定义
            String funcName = ctx.IDENT().getText();
            Symbol funcInTable = currentScope.resolve(funcName);
            if(funcInTable == null){
                errorTable.addErrorTable(getLineNo(ctx),2);
                return  null;
            }
            // 报告 Error type 10 对变量使用函数调用
            else if(funcInTable instanceof VariableSymbol){
                errorTable.addErrorTable(getLineNo(ctx),10);
                return null;
            }
            // 报告 Error type 8 函数参数不适用
            else if(funcInTable instanceof FunctionSymbol) {
                String LFuncType = ((FunctionSymbol) funcInTable).getType().toString();
                LFuncType = LFuncType.substring(LFuncType.indexOf('('));
                String RFuncType = "(";
                int RFuncParamsNum = 0;
                if(ctx.funcRParams() != null)
                   RFuncParamsNum = ctx.funcRParams().param().size();
                for (int i=0;i<RFuncParamsNum;i++){
                    RFuncType = RFuncType +  getExpType(ctx.funcRParams().param(i).exp()).toString();
                }

                RFuncType = RFuncType + ")";

//                System.out.println("LFuncType: " +LFuncType + "; RFuncType: " + RFuncType);
                if(! LFuncType.equals(RFuncType)){
                    errorTable.addErrorTable(getLineNo(ctx),8);
                    return null;
                }
            }
        } else if (ctx.unaryOp() != null) { // unaryOp exp
            // 报告 Error type 6 运算符需求类型与提供类型不匹配
            String Rtype = getExpType(ctx.exp(0)).toString();
            if (! Rtype.equals("int") ){
                errorTable.addErrorTable(getLineNo(ctx),6);
                return null;
            }
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            // 报告 Error type 6 运算符需求类型与提供类型不匹配
            String Ltype = getExpType(ctx.exp(0)).toString();
            String Rtype = getExpType(ctx.exp(1)).toString();
            if (! ( Ltype.equals("int") && Rtype.equals("int") ) ){
                errorTable.addErrorTable(getLineNo(ctx),6);
                return null;
            }
        }
        return super.visitExp(ctx);
    }

    @Override
    public Void visitCond(SysYParser.CondContext ctx) {
        // 报告 Error type 6 运算符需求类型与提供类型不匹配
        if(ctx.exp()!=null){
            String type = getExpType(ctx.exp()).toString();
            if (! type.equals("int") ){
                errorTable.addErrorTable(getLineNo(ctx),6);
                return null;
            }
        }
        return super.visitCond(ctx);
    }

    public boolean getErrorStatus() {
        return errorTable.isError_Status();
    }

    public void setErrorStatus(boolean bool) {
        errorTable.setError_Status(bool);
    }

}

