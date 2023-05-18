
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import scope.GlobalScope;
import symbol.*;
import scope.*;
import type.*;

import java.util.ArrayList;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMGlobalVisitor extends SysYParserBaseVisitor<LLVMValueRef> {

    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private int localScopeCounter = 0;


    LLVMModuleRef module;
    LLVMBuilderRef builder;
    LLVMTypeRef i32Type;

    @Override
    public LLVMValueRef visit(ParseTree tree) {
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();

        //创建module
        this.module = LLVMModuleCreateWithName("moudle");
        //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
        this.builder = LLVMCreateBuilder();
        //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
        this.i32Type = LLVMInt32Type();


        // 遍历子树
        LLVMValueRef llvmValueRef = tree.accept(this);

        //输出到控制台
//        LLVMDumpModule(module);
        //输出到文件
        BytePointer error = new BytePointer();
//        if (LLVMPrintModuleToFile(module, Main.argsCopy[1]+"test.ll", error) != 0) {    // moudle是你自定义的LLVMModuleRef对象
        if (LLVMPrintModuleToFile(module, Main.argsCopy[1], error) != 0) {    // moudle是你自定义的LLVMModuleRef对象
            LLVMDisposeMessage(error);
        }

        return llvmValueRef;
    }

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        // 进入新的 Scope
        globalScope = new GlobalScope(null);
        currentScope = globalScope;
        // 遍历子树
        LLVMValueRef ret = super.visitProgram(ctx);
        // 回到上一层 Scope
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    @Override
    public LLVMValueRef visitCompUnit(SysYParser.CompUnitContext ctx) {
        return super.visitCompUnit(ctx);
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        String typeName = ctx.funcType().getText();

        Symbol funcSymbolInTable = globalScope.resolve(funcName);

        // 不需要修复错误。进入新的 Scope，定义新的 Symbol
        FunctionSymbol fun = new FunctionSymbol(funcName, currentScope);
        fun.setFunctionType((Type) globalScope.resolve(typeName),new ArrayList<>());// 具体参数待进入FParam再填入

        // 是scope也是symbol,需要放到符号表里
        currentScope.define(fun);
        currentScope = fun;

        //生成返回值类型
        LLVMTypeRef returnType = i32Type;

        //生成函数参数类型
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(0);

        //生成函数类型
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ 0, /* isVariadic */ 0);
        //若仅需一个参数也可以使用如下方式直接生成函数类型
//        ft = LLVMFunctionType(returnType, i32Type, /* argumentCount */ 0, /* isVariadic */ 0);

        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/funcName, ft);

        //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
        LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, /*blockName:String*/funcName + "Entry");
        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block1);

        // 遍历子树
        LLVMValueRef res = super.visitFuncDef(ctx);
        // 回到上一层 Scope
        currentScope = currentScope.getEnclosingScope();

        return res;
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        // 进入新的 Scope
        LocalScope localScope = new LocalScope(currentScope);
        String localScopeName = localScope.getName() + localScopeCounter;
        localScope.setName(localScopeName);
        localScopeCounter++;
        currentScope = localScope;

        // 遍历子树
        LLVMValueRef ret = super.visitBlock(ctx);
        // 回到上一层 Scope
        currentScope = currentScope.getEnclosingScope();
        return ret;
    }

    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            String varName = varDefContext.IDENT().getText();
            Symbol varNameInTable = currentScope.resolveInScope(varName);
            assert (varNameInTable == null);

            String typeName = ctx.bType().getText();
            Type type = (Type) globalScope.resolve(typeName);

//             定义新的 Symbol
            VariableSymbol varSymbol = new VariableSymbol(varName, type);
            currentScope.define(varSymbol);

//             存入LLVMVALUE
            if (varDefContext.ASSIGN() != null) {
                LLVMValueRef initVal = visitInitVal(varDefContext.initVal());
                // 全局变量创建
                if (currentScope == globalScope) {
                    //创建名为globalVar的全局变量
                    LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/varName);
                    //为全局变量设置初始化器
                    LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/initVal);
                    varSymbol.setNumber(globalVar);
                }
                // 局部变量创建
                else {
                    LLVMValueRef currentVar = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName);
                    //将数值存入该内存
                    LLVMBuildStore(builder, initVal, currentVar);
                    varSymbol.setNumber(currentVar);
                }
            }
            else {
                LLVMValueRef currentVar = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName);
                varSymbol.setNumber(currentVar);
            }

        }
        return super.visitVarDecl(ctx);
    }

    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {
        for (SysYParser.ConstDefContext varDefContext : ctx.constDef()) {
            String varName = varDefContext.IDENT().getText();
            Symbol varNameInTable = currentScope.resolveInScope(varName);
            assert (varNameInTable == null);

            String typeName = ctx.bType().getText();
            Type type = (Type) globalScope.resolve(typeName);

//             定义新的 Symbol
            VariableSymbol varSymbol = new VariableSymbol(varName, type);
            currentScope.define(varSymbol);

//             存入LLVMVALUE
            if (varDefContext.ASSIGN() != null) {
                LLVMValueRef initVal = getConstInitVal(varDefContext.constInitVal());
                // 全局变量创建
                if (currentScope == globalScope) {
                    //创建名为globalVar的全局变量
                    LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/varName);
                    //为全局变量设置初始化器
                    LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/initVal);
                    varSymbol.setNumber(globalVar);
                }
                // 局部变量创建
                else {
                    LLVMValueRef currentVar = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName);
                    //将数值存入该内存
                    LLVMBuildStore(builder, initVal, currentVar);
                    varSymbol.setNumber(currentVar);
                }
            }
        }

        return super.visitConstDecl(ctx);
    }

    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        if(ctx.ASSIGN()!=null){
            LLVMValueRef Lval = getLVal(ctx.lVal());
            LLVMValueRef Rval = visitExp(ctx.exp());
            LLVMBuildStore(builder, Rval, Lval);
        }
        else if (ctx.RETURN() != null){
            LLVMValueRef retValue = visitExp(ctx.exp());
            //函数返回指令
            LLVMBuildRet(builder, /*result:LLVMValueRef*/retValue);
            return null;
        }
        return super.visitStmt(ctx);
    }


    public LLVMValueRef getConstInitVal(SysYParser.ConstInitValContext ctx) {
        if (ctx.constExp()!=null){
            return visitExp(ctx.constExp().exp());
        }
        else {

        }

        assert (ctx.constExp()!=null); // 抛出异常
        return null;
    }

    @Override
    public LLVMValueRef visitInitVal(SysYParser.InitValContext ctx) {
        if(ctx.L_BRACE()!=null) {

        }
        else {
            return visitExp(ctx.exp());
        }
        return super.visitInitVal(ctx);
    }

    @Override
    public LLVMValueRef visitExp(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN

        } else if (ctx.L_PAREN() != null) { // L_PAREN exp R_PAREN
            return visitExp(ctx.exp(0));
        } else if (ctx.unaryOp() != null) { // unaryOp exp
            LLVMValueRef RNum = visitExp(ctx.exp(0));
            LLVMValueRef result = RNum;
            if(ctx.unaryOp().MINUS()!=null){
                result = LLVMBuildNeg(builder, RNum, "neg_");
            }
            else if(ctx.unaryOp().PLUS()!=null){
                result = RNum;
            }
            else if(ctx.unaryOp().NOT()!=null){
//                result = LLVMBuildNot(builder, RNum, "not_");
                result=LLVMBuildICmp(builder, LLVMIntEQ, LLVMConstInt(i32Type, 0, 0), RNum, "not_");
                result = LLVMBuildZExt(builder, result, i32Type, "zext_");
            }
            return result;
        } else if (ctx.lVal() != null) { // lVal
            return visitLVal(ctx.lVal());
        } else if (ctx.number() != null) { // number
            long num = Integer.parseInt(Main.HEXtoTEN(ctx.number().getText()));
            //创建一个常量
            LLVMValueRef tempNum = LLVMConstInt(i32Type, num, /* signExtend */ 0);
            return tempNum;
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null)
        {
            LLVMValueRef result = null;
            LLVMValueRef LNum = visitExp(ctx.exp(0));
            LLVMValueRef RNum = visitExp(ctx.exp(1));
            if (ctx.MUL() != null){
                result = LLVMBuildMul(builder,LNum,RNum,"mul_");
            }
            else if (ctx.DIV() != null){// s:有符号整数
                result = LLVMBuildSDiv(builder,LNum,RNum,"sdiv_");
            }
            else if (ctx.MOD() != null){ // s:有符号整数
                result = LLVMBuildSRem(builder,LNum,RNum,"srem_");
            }
            return result;
        }else if(ctx.PLUS() != null || ctx.MINUS() != null){
            LLVMValueRef result = null;
            LLVMValueRef LNum = visitExp(ctx.exp(0));
            LLVMValueRef RNum = visitExp(ctx.exp(1));
            if (ctx.PLUS() != null){
                result = LLVMBuildAdd(builder,LNum,RNum,"add_");
            }
            else{// s:有符号整数
                result = LLVMBuildSub(builder,LNum,RNum,"sub_");
            }
            return result;
        }
        return null;
    }


    public LLVMValueRef getLVal(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        Symbol varInTable = currentScope.resolve(varName);
        assert (varInTable!=null);

        assert (varInTable instanceof VariableSymbol);

        LLVMValueRef res = ((VariableSymbol) varInTable).getNumber();
//        LLVMValueRef value = LLVMBuildLoad(builder, res, /*varName:String*/varInTable.getName());


        return res;
    }

    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        Symbol varInTable = currentScope.resolve(varName);
        assert (varInTable!=null);

        assert (varInTable instanceof VariableSymbol);

        LLVMValueRef res = ((VariableSymbol) varInTable).getNumber();
        LLVMValueRef value = LLVMBuildLoad(builder, res, /*varName:String*/varInTable.getName());


        return value;
    }

    @Override
    public LLVMValueRef visitFuncType(SysYParser.FuncTypeContext ctx) {
        return super.visitFuncType(ctx);
    }

    @Override
    public LLVMValueRef visitTerminal(TerminalNode node) {
        return super.visitTerminal(node);
    }
}
