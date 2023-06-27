
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
import java.util.List;
import java.util.Stack;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMGlobalVisitor extends SysYParserBaseVisitor<LLVMValueRef> {

    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private int localScopeCounter = 0;

    // Entry指出口
    private ArrayList<LLVMBasicBlockRef> whileEntryBlock = new ArrayList<>();
    private ArrayList<LLVMBasicBlockRef> whileCondBlock = new ArrayList<>();
    private int whileBlockIdx = -1;
    private Stack<LLVMBasicBlockRef> shortCircleFalseBlock = new Stack<>();
    private Stack<LLVMBasicBlockRef> shortCircleTrueBlock = new Stack<>();
    private int shortCircleBlockIdx = 0;
    private ArrayList<LLVMBasicBlockRef> fixedBlock = new ArrayList<>();

    LLVMModuleRef module;
    LLVMBuilderRef builder;

    LLVMTypeRef ppI32Type;
    LLVMTypeRef pI32Type;
    LLVMTypeRef i32Type;
    LLVMTypeRef i1Type;
    LLVMTypeRef voidType;
    LLVMValueRef zero;
    LLVMValueRef i1True;
    LLVMValueRef i1False;
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
        this.pI32Type = LLVMPointerType(i32Type, 0);
        this.ppI32Type = LLVMPointerType(pI32Type, 0);
        this.i1Type = LLVMInt1Type();
        this.voidType = LLVMVoidType();
        this.zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
        this.i1True = LLVMConstInt(i1Type, 1, /* signExtend */ 0);
        this.i1False = LLVMConstInt(i1Type, 0, /* signExtend */ 0);

        // 遍历子树
        LLVMValueRef llvmValueRef = tree.accept(this);


        for (LLVMBasicBlockRef llvmBasicBlockRef : fixedBlock) {
            LLVMPositionBuilderAtEnd(builder, llvmBasicBlockRef);
            LLVMBuildBr(builder, llvmBasicBlockRef);
        }

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
        assert (funcSymbolInTable==null);

        // 不需要修复错误。进入新的 Scope，定义新的 Symbol
        FunctionSymbol fun = new FunctionSymbol(funcName, currentScope);
        fun.setFunctionType((Type) globalScope.resolve(typeName),new ArrayList<>());// 具体参数待进入FParam再填入

        // 是scope也是symbol,需要放到符号表里
        currentScope.define(fun);
        currentScope = fun;

        LLVMTypeRef returnType = null;
        if(typeName.equals("int")) {
            //生成返回值类型
             returnType = i32Type;
        }
        else if(typeName.equals("void")){
            returnType = voidType;
        }
        else {
            throw new RuntimeException("funcType Error");
        }

        //生成函数参数类型
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(0);
        int paramNum = 0;
        if(ctx.funcFParams()!=null) {
            for (SysYParser.FuncFParamContext paramCtx : ctx.funcFParams().funcFParam()) {
                if (paramCtx.L_BRACKT().size() == 0) {
                    argumentTypes.put(paramNum, i32Type);
                }
                else {
                    argumentTypes.put(paramNum, pI32Type);
                }
                paramNum++;
            }
        }
        //生成函数类型
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ paramNum, /* isVariadic */ 0);
        //若仅需一个参数也可以使用如下方式直接生成函数类型
//        ft = LLVMFunctionType(returnType, i32Type, /* argumentCount */ 0, /* isVariadic */ 0);

        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/funcName, ft);
        fun.setFuncRef(function);

        //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
        LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, /*blockName:String*/funcName + "Entry");
        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block1);

        // 遍历子树
        LLVMValueRef res = super.visitFuncDef(ctx);
        // 回到上一层 Scope
        LLVMTypeRef funcReturnType = LLVMGetReturnType(LLVMGetElementType(LLVMTypeOf(getCurrentFunc())));

        if (funcReturnType.equals(voidType)){
            LLVMBuildRetVoid(builder);
        }
        LLVMBuildRet(builder, zero);
        currentScope = currentScope.getEnclosingScope();

        return res;
    }

    @Override
    public LLVMValueRef visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        int paramIdx = 0;
        for (SysYParser.FuncFParamContext paramCtx : ctx.funcFParam()) {
            String varName = paramCtx.IDENT().getText();
            String typeName = paramCtx.bType().getText();
            Type type = (Type) globalScope.resolve(typeName);
            assert (type instanceof BasicTypeSymbol);

            VariableSymbol varSymbol = new VariableSymbol(varName, type);
            currentScope.define(varSymbol);

            assert (currentScope instanceof FunctionSymbol);
            LLVMValueRef funcLLVM = ((FunctionSymbol) currentScope).getFuncRef();
            // 获取函数参数
            LLVMValueRef param = LLVMGetParam(funcLLVM, /* parameterIndex */ paramIdx);
            // 为参数分配内存
            LLVMValueRef funcVar = null;
            if(paramCtx.L_BRACKT().size() != 0) {
                funcVar = LLVMBuildAlloca(builder, pI32Type, /*pointerName:String*/varName);
            }
            else {
                funcVar = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName);
            }

            // 将数值存入该内存
            LLVMBuildStore(builder, param, funcVar);
            varSymbol.setNumber(funcVar);
            varSymbol.setIntType(ppI32Type);

            paramIdx++;
        }
        return super.visitFuncFParams(ctx);
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
            LLVMValueRef initVal = null;
            if (varDefContext.ASSIGN() != null) {
                 initVal = visitInitVal(varDefContext.initVal());
            }
            else {
                initVal = zero;
            }
            // 全局变量创建
            createVar(varSymbol, initVal, varDefContext.L_BRACKT(), varDefContext.constExp());
            // 单独处理数组赋值
            if(varDefContext.ASSIGN() != null && varDefContext.L_BRACKT().size()!=0) {
                LLVMValueRef[] initArray = getInitArray(varDefContext.initVal());
                copyArrToArrPtr( varSymbol.getNumber(), initArray);
            }
        }

//        return super.visitVarDecl(ctx);
        return null;
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
            assert  (varDefContext.ASSIGN() != null) ;
            LLVMValueRef initVal = getConstInitVal(varDefContext.constInitVal());
            // 全局变量创建
            createVar( varSymbol, initVal, varDefContext.L_BRACKT(), varDefContext.constExp());
            // 单独处理数组赋值
            if(varDefContext.L_BRACKT().size()!=0) {
                LLVMValueRef[] initArray = getConstInitArray(varDefContext.constInitVal());
                copyArrToArrPtr( varSymbol.getNumber(), initArray);
            }
        }

//        return super.visitConstDecl(ctx);
        return null;
    }

    // 单独处理数组的初始化
    private LLVMValueRef[] getInitArray(SysYParser.InitValContext initValContext){
        int arrLen = initValContext.initVal().size();
        LLVMValueRef[] initArray = new LLVMValueRef[arrLen];
        for(int i = 0; i < arrLen; i++) {
            initArray[i] = visitInitVal(initValContext.initVal(i));
        }
        return initArray;
    }

    private LLVMValueRef[] getConstInitArray(SysYParser.ConstInitValContext initValContext){
        int arrLen = initValContext.constInitVal().size();
        LLVMValueRef[] initArray = new LLVMValueRef[arrLen];
        for(int i = 0; i < arrLen; i++) {
            initArray[i] = getConstInitVal(initValContext.constInitVal(i));
        }
        return initArray;
    }

    // 数组赋值
    private void copyArrToArrPtr( LLVMValueRef arrayPointer, LLVMValueRef[] initArray) {
        int arraySize = initArray.length;
        LLVMValueRef[] arrayIndex = new LLVMValueRef[2];
        arrayIndex[0] = zero;
        for (int i = 0; i < arraySize; i++) {
            arrayIndex[1] = LLVMConstInt(i32Type, i, 0);
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayIndex);
            LLVMValueRef elementPtr = LLVMBuildGEP(builder, arrayPointer, indexPointer, 2, "ArrayPtr_"+i);
            LLVMBuildStore(builder, initArray[i], elementPtr);
        }
    }

    private void createVar(VariableSymbol varSymbol, LLVMValueRef initVal, List<TerminalNode> arrayNode, List<SysYParser.ConstExpContext> constExpContext) {
        String varName = varSymbol.getName();
        if (currentScope == globalScope) {
            //创建名为globalVar的全局变量
            if(arrayNode.size() == 0) { // 普通变量
                LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/varName);
                //为全局变量设置初始化器
                LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/initVal);
                varSymbol.setNumber(globalVar);
            }
            else { // 数组变量
                int size = Integer.parseInt(constExpContext.get(0).exp().number().getText());
                LLVMValueRef globalVar = LLVMAddGlobal(module, LLVMArrayType(i32Type, size), /*globalVarName:String*/varName);
                //为全局变量设置初始化器
                PointerPointer<Pointer> pointerPointer = new PointerPointer<>(size);
                for (int i = 0; i < size; ++i) {
                    pointerPointer.put(i, zero);
                }
                LLVMValueRef initArray = LLVMConstArray(i32Type, pointerPointer, size);
                LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/initArray); // 初始化全局数组？
                varSymbol.setNumber(globalVar); // 存入全局数组pInt
                varSymbol.setIntType(pI32Type);
            }
        }
        // 局部变量创建
        else {
            if(arrayNode.size() == 0) { // 普通变量
                LLVMValueRef currentVar = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName);
                //将数值存入该内存
                LLVMBuildStore(builder, initVal, currentVar);
                varSymbol.setNumber(currentVar);
            }
            else { // 数组变量
                int size = Integer.parseInt(constExpContext.get(0).exp().number().getText());
                LLVMValueRef currentVar = LLVMBuildAlloca(builder, LLVMArrayType(i32Type, size), /*pointerName:String*/varName);
                //将数值存入该内存
                    //为全局变量设置初始化器
                PointerPointer<Pointer> pointerPointer = new PointerPointer<>(size);
                for (int i = 0; i < size; ++i) {
                    pointerPointer.put(i, zero);
                }
                LLVMValueRef initArray = LLVMConstArray(i32Type, pointerPointer, size);
                LLVMBuildStore(builder, initArray, currentVar); // todo: 初始化局部数组？
                varSymbol.setNumber(currentVar);
                varSymbol.setIntType(pI32Type);
            }
        }
    }


    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        if(ctx.ASSIGN()!=null){
            LLVMValueRef Lval = getLVal(ctx.lVal());
            LLVMValueRef Rval = visitExp(ctx.exp());
            LLVMBuildStore(builder, Rval, Lval);
        }
        else if (ctx.RETURN() != null){
            LLVMTypeRef funcReturnType = LLVMGetReturnType(LLVMGetElementType(LLVMTypeOf(getCurrentFunc())));

            if (funcReturnType.equals(i32Type)){
                LLVMValueRef retValue = visitExp(ctx.exp());
                LLVMBuildRet(builder, retValue);
            }
        }
        else if(ctx.IF()!=null){
            LLVMValueRef function = getCurrentFunc();
            LLVMBasicBlockRef ifTrue = LLVMAppendBasicBlock(function, /*blockName:String*/"ifTrue");
            LLVMBasicBlockRef ifFalse = LLVMAppendBasicBlock(function, /*blockName:String*/"ifFalse");
            LLVMBasicBlockRef entry = LLVMAppendBasicBlock(function, /*blockName:String*/"ifEntry");
            //条件跳转指令，选择跳转到哪个块

            shortCircleTrueBlock.push(ifTrue);
            shortCircleFalseBlock.push(ifFalse);
            LLVMValueRef condition = visitCond(ctx.cond());

            // 短路求值不需要再添跳转
            if(condition!=null) {
                condition = condI32ToI1(condition);
                LLVMBuildCondBr(builder,
                        /*condition:LLVMValueRef*/ condition,
                        /*ifTrue:LLVMBasicBlockRef*/ ifTrue,
                        /*ifFalse:LLVMBasicBlockRef*/ ifFalse);
            }

            shortCircleTrueBlock.pop();
            shortCircleFalseBlock.pop();

            // 生成ifTrue ifFalse的指令
            LLVMPositionBuilderAtEnd(builder, ifTrue);//后续生成的指令将追加在后面
            visitStmt(ctx.stmt(0));
            LLVMBuildBr(builder, entry);
            LLVMPositionBuilderAtEnd(builder, ifFalse);//后续生成的指令将追加在后面
            if(ctx.ELSE()!=null) {
                visitStmt(ctx.stmt(1));
            }
            LLVMBuildBr(builder, entry);
            LLVMPositionBuilderAtEnd(builder, entry);//后续生成的指令将追加在后面

        }
        else if(ctx.WHILE()!=null){
            LLVMValueRef function = getCurrentFunc();
            LLVMBasicBlockRef whileCond = LLVMAppendBasicBlock(function, /*blockName:String*/"whileCond");
            LLVMBasicBlockRef whileBody = LLVMAppendBasicBlock(function, /*blockName:String*/"whileBody");
            LLVMBasicBlockRef entry = LLVMAppendBasicBlock(function, /*blockName:String*/"whileEntry");


            whileCondBlock.add(whileCond);
            whileEntryBlock.add(entry);
            whileBlockIdx++;

            LLVMBuildBr(builder, whileCond);
            LLVMPositionBuilderAtEnd(builder, whileCond);//whileCond后续生成的指令将追加在后面

            shortCircleTrueBlock.push(whileBody);
            shortCircleFalseBlock.push(entry);
            LLVMValueRef condition = visitCond(ctx.cond());

            // 短路求值不需要再添跳转
            if(condition!=null) {
                condition = condI32ToI1(condition);
                LLVMBuildCondBr(builder,
                        /*condition:LLVMValueRef*/ condition,
                        /*ifTrue:LLVMBasicBlockRef*/ whileBody,
                        /*ifFalse:LLVMBasicBlockRef*/ entry);
            }

            shortCircleTrueBlock.pop();
            shortCircleFalseBlock.pop();

            LLVMPositionBuilderAtEnd(builder, whileBody);//whileBody后续生成的指令将追加在后面
            visitStmt(ctx.stmt(0));
            whileBlockIdx--;
            LLVMBuildBr(builder, whileCond);

            LLVMPositionBuilderAtEnd(builder, entry);//后续生成的指令将追加在后面
        }
        else if(ctx.BREAK()!=null){
            LLVMBasicBlockRef whileEntry = whileEntryBlock.get(whileBlockIdx);
            LLVMBuildBr(builder, whileEntry);
        }
        else if (ctx.CONTINUE()!=null){
            LLVMBasicBlockRef whileCond = whileCondBlock.get(whileBlockIdx);
            LLVMBuildBr(builder, whileCond);
        }
        else if(ctx.block()!=null){
            visitBlock(ctx.block());
        }

        return null;
    }

    @Override
    public LLVMValueRef visitCond(SysYParser.CondContext ctx) {
        if(ctx.exp()!=null){
            LLVMValueRef condition = visitExp(ctx.exp());
            if(i1Type.equals(LLVMTypeOf(condition))){
                condition = LLVMBuildZExt(builder, condition, i32Type, "cond_");
            }
            assert (condition!=null); // 抛出异常
            return condition;
        }
        else{
            LLVMValueRef condition = null;

            if(ctx.AND()!=null || ctx.OR() != null){
                // 保存当前函数上一层
                LLVMValueRef function = getCurrentFunc();
                LLVMBasicBlockRef leftCondExit = LLVMAppendBasicBlock(function, /*blockName:String*/"leftCondExit"); // 中间
                LLVMBasicBlockRef lastTrueBlock = shortCircleTrueBlock.peek(); // 上层正确
                LLVMBasicBlockRef lastFalseBlock = shortCircleFalseBlock.peek(); // 上层错误
                shortCircleBlockIdx++;

                if (ctx.AND()!=null){

                    LLVMBasicBlockRef leftCondExitTrue = LLVMAppendBasicBlock(function, /*blockName:String*/"leftCondExitTrue");
                    fixedBlock.add(leftCondExitTrue);
                    shortCircleTrueBlock.push(leftCondExitTrue);
                    // 如果下一层正确，则跳转到中间（leftCondExitTrue）
                    // 如果下一层错误，则跳转到上一层错误（AND的特性）。之前出bug就是因为多了一个false块

                    LLVMValueRef Lcond = visitCond(ctx.cond(0));
                    if(Lcond != null) {  // 左节点为表达式
                        Lcond = condI32ToI1(Lcond);
                        LLVMBuildCondBr(builder,
                                /*condition:LLVMValueRef*/ Lcond,
                                /*ifTrue:LLVMBasicBlockRef*/ leftCondExit,      // 到中间
                                /*ifFalse:LLVMBasicBlockRef*/ lastFalseBlock); // 回到上一层节点，返回错误
                    }
                    else { // 左节点嵌套
                        // if Lcond false
                        LLVMPositionBuilderAtEnd(builder, shortCircleFalseBlock.peek());
                        LLVMBuildBr(builder,lastFalseBlock);
                        LLVMPositionBuilderAtEnd(builder, leftCondExitTrue);
                        LLVMBuildBr(builder,leftCondExit);
                    }
                    shortCircleTrueBlock.pop(); // 在最后pop也行。不影响lastTrueBlock

                    // if Lcond true
                    LLVMPositionBuilderAtEnd(builder, leftCondExit);//
                    LLVMValueRef Rcond = visitCond(ctx.cond(1));
                    if(Rcond != null) { // 右节点为表达式
                        Rcond = condI32ToI1(Rcond);
                        LLVMBuildCondBr(builder,
                                /*condition:LLVMValueRef*/ Rcond,
                                /*ifTrue:LLVMBasicBlockRef*/ lastTrueBlock, // 回到上一层，返回正确
                                /*ifFalse:LLVMBasicBlockRef*/ lastFalseBlock); // 回到上一层，返回错误

                    }

                    shortCircleBlockIdx--;
                    return null;
                }
                else {  // OR != NULL
//                    condition = LLVMBuildOr(builder, Lcond, Rcond, "or_");
                    LLVMBasicBlockRef leftCondExitFalse = LLVMAppendBasicBlock(function, /*blockName:String*/"leftCondExitFalse");
                    fixedBlock.add(leftCondExitFalse);
                    shortCircleFalseBlock.push(leftCondExitFalse);
                    // 如果下一层错误，则跳转到中间（leftCondExitFalse）
                    // 如果下一层正确，则跳转到上一层正确（OR的特性）之前出bug就是因为多了一个true块

                    LLVMValueRef Lcond = visitCond(ctx.cond(0));
                    if(Lcond != null) { // 左节点为表达式
                        Lcond = condI32ToI1(Lcond);
                        LLVMBuildCondBr(builder,
                                /*condition:LLVMValueRef*/ Lcond,
                                /*ifTrue:LLVMBasicBlockRef*/ lastTrueBlock,      // 回到上一层节点，返回正确
                                /*ifFalse:LLVMBasicBlockRef*/ leftCondExit); // 到中间
                    }
                    else { // 左节点嵌套
                        // if Lcond false
                        LLVMPositionBuilderAtEnd(builder, leftCondExitFalse);
                        LLVMBuildBr(builder,leftCondExit);
                        LLVMPositionBuilderAtEnd(builder, shortCircleTrueBlock.peek());
                        LLVMBuildBr(builder,lastTrueBlock);
                    }

                    shortCircleFalseBlock.pop();

                    // if Lcond true
                    LLVMPositionBuilderAtEnd(builder, leftCondExit);//
                    LLVMValueRef Rcond = visitCond(ctx.cond(1));
                    if(Rcond != null) { // 右节点表达式
                        Rcond = condI32ToI1(Rcond);
                        LLVMBuildCondBr(builder,
                                /*condition:LLVMValueRef*/ Rcond,
                                /*ifTrue:LLVMBasicBlockRef*/ lastTrueBlock, // 回到上一层，返回正确
                                /*ifFalse:LLVMBasicBlockRef*/ lastFalseBlock); // 回到上一层，返回右cond错误(关键bug，默认自己是左cond)
                    }

                    shortCircleBlockIdx--;
                    return null;
                }
            }
            else if(ctx.EQ()!=null || ctx.NEQ()!=null){
                LLVMValueRef Lcond = visitCond(ctx.cond(0));
                LLVMValueRef Rcond = visitCond(ctx.cond(1));
                //生成比较指令
                if (ctx.EQ()!=null){
                    condition = LLVMBuildICmp
                            (builder, /*这是个int型常量，表示比较的方式*/LLVMIntEQ, Lcond, Rcond, "eq_");

                }
                else {
                    condition = LLVMBuildICmp
                            (builder, /*这是个int型常量，表示比较的方式*/LLVMIntNE, Lcond, Rcond, "neq_");
                }
            }
            else {
                LLVMValueRef Lcond = visitCond(ctx.cond(0));
                LLVMValueRef Rcond = visitCond(ctx.cond(1));
                if (ctx.LT()!=null){
                    condition = LLVMBuildICmp
                            (builder, /*这是个int型常量，表示比较的方式*/LLVMIntSLT, Lcond, Rcond, "slt_");
                }
                else if(ctx.GT()!=null){
                    condition = LLVMBuildICmp
                            (builder, /*这是个int型常量，表示比较的方式*/LLVMIntSGT, Lcond, Rcond, "sgt_");
                }
                else if(ctx.LE()!=null){
                    condition = LLVMBuildICmp
                            (builder, /*这是个int型常量，表示比较的方式*/LLVMIntSLE, Lcond, Rcond, "sle_");
                }
                else if(ctx.GE()!=null){
                    condition = LLVMBuildICmp
                            (builder, /*这是个int型常量，表示比较的方式*/LLVMIntSGE, Lcond, Rcond, "sge_");
                }
            }
            if(i1Type.equals(LLVMTypeOf(condition))){
                condition = LLVMBuildZExt(builder, condition, i32Type, "cond_");
            }
            assert (condition!=null); // 抛出异常
            return condition;
        }

    }

    public LLVMValueRef condI32ToI1(LLVMValueRef condition){
            assert (condition!=null); // 抛出异常
            condition = LLVMBuildZExt(builder, condition, i32Type, "cond_");
            condition = LLVMBuildICmp
                    (builder, /*这是个int型常量，表示比较的方式*/LLVMIntNE, zero, condition, "cond_");
            return condition;
    }

    // 短路求值
    public LLVMValueRef shortCircuit(LLVMValueRef condition){
        assert (condition!=null); // 抛出异常

        condition = LLVMBuildZExt(builder, condition, i32Type, "cond_");
        return condition;
    }

    public LLVMValueRef getCurrentFunc(){
        Scope tempScope = currentScope;
        while (!(tempScope instanceof FunctionSymbol) ){
            tempScope = tempScope.getEnclosingScope();
        }
        assert (tempScope instanceof FunctionSymbol); // 抛出异常
        return ((FunctionSymbol) tempScope).getFuncRef();
    }

    public LLVMValueRef getConstInitVal(SysYParser.ConstInitValContext ctx) {
        if (ctx.L_BRACE()!=null){
            return null;
        }
        else {
            return visitExp(ctx.constExp().exp());
        }

//        return null;
    }

    @Override
    public LLVMValueRef visitInitVal(SysYParser.InitValContext ctx) {
        if(ctx.L_BRACE()!=null) {
//            LLVMValueRef array = LLVMArrayType()
            return null;
        }
        else {
            return visitExp(ctx.exp());
        }
//        return super.visitInitVal(ctx);
    }

     public LLVMValueRef[] getFuncRParams(SysYParser.FuncRParamsContext ctx) {
         LLVMValueRef[] args = new LLVMValueRef[ctx.param().size()];
         for (int i = 0; i < ctx.param().size(); i++) {
             LLVMValueRef exp = visitExp(ctx.param(i).exp());
//             LLVMValueRef[] arrayIndex = new LLVMValueRef[0];
//            // arrayIndex[0] = zero;
//             PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayIndex);
//             LLVMValueRef expPtr = LLVMBuildGEP(builder, exp, indexPointer, 0, "expPtr");
//             System.out.println(LLVMTypeOf(expPtr).equals(LLVMPointerType(LLVMInt32Type(), 0)));
//             if(LLVMGetElementType(LLVMTypeOf(exp)).equals(LLVMArrayType(LLVMInt32Type(), 5))){
//                args[i] = getLVal(ctx.param(i).exp().lVal());
//             }
//             else {
                args[i] = exp;
//             }
         }
         return args;
     }


    @Override
    public LLVMValueRef visitExp(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN
            String funcName = ctx.IDENT().getText();
            FunctionSymbol funcSymbol = (FunctionSymbol) globalScope.resolve(funcName);
            assert (funcSymbol != null);
            LLVMValueRef func = funcSymbol.getFuncRef();
            assert (func != null);
            LLVMValueRef[] args = new LLVMValueRef[0];
            if (ctx.funcRParams() != null) {
                args = getFuncRParams(ctx.funcRParams());
            }
            return LLVMBuildCall(builder, func, new PointerPointer(args), args.length, "call_");
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


    // 返回Lval, 无Load, 修改对原值有效
    public LLVMValueRef getLVal(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        Symbol varInTable = currentScope.resolve(varName);
        assert (varInTable!=null);

        assert (varInTable instanceof VariableSymbol);

        LLVMValueRef res = ((VariableSymbol) varInTable).getNumber();
//        LLVMValueRef value = LLVMBuildLoad(builder, res, /*varName:String*/varInTable.getName());
        if(ctx.L_BRACKT().size()!=0){ // 取出数组
//            res = LLVMBuildLoad(builder,res,"arr");
            if(((VariableSymbol) varInTable).getIntType().equals(ppI32Type)) {
                res = LLVMBuildLoad(builder, res, "arr");
                LLVMValueRef[] arrayPointer = new LLVMValueRef[1];
                arrayPointer[0] = visitExp(ctx.exp(0));
                res = LLVMBuildGEP(builder, res, new PointerPointer(arrayPointer), arrayPointer.length, "arr_LNum");
            }
            else {
                LLVMValueRef[] arrayPointer = new LLVMValueRef[2];
                arrayPointer[0] = zero;
                arrayPointer[1] = visitExp(ctx.exp(0));
                res = LLVMBuildGEP(builder, res, new PointerPointer(arrayPointer), arrayPointer.length, "arr_LNum");
            }
        }

        return res;
    }

    // 返回Load出来的Value, 修改对原值无效
    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        Symbol varInTable = currentScope.resolve(varName);

        assert (varInTable instanceof VariableSymbol);

        LLVMValueRef res = ((VariableSymbol) varInTable).getNumber();
        LLVMValueRef value = null;
        if(ctx.L_BRACKT().size()!=0){ // 取出数组
            if(((VariableSymbol) varInTable).getIntType().equals(ppI32Type)) {
                res = LLVMBuildLoad(builder, res, "arr");
                LLVMValueRef[] arrayPointer = new LLVMValueRef[1];
                arrayPointer[0] = visitExp(ctx.exp(0));
                res = LLVMBuildGEP(builder, res, new PointerPointer(arrayPointer), arrayPointer.length, "arrNum");
            }
            else {
                LLVMValueRef[] arrayPointer = new LLVMValueRef[2];
                arrayPointer[0] = zero;
                arrayPointer[1] = visitExp(ctx.exp(0));
                res = LLVMBuildGEP(builder, res, new PointerPointer(arrayPointer), arrayPointer.length, "arrNum");
            }
        }

        // 特判数组传指针
        if(ctx.L_BRACKT().size()==0 && ((VariableSymbol) varInTable).getIntType().equals(pI32Type)){
            LLVMValueRef[] arrayIndex = new LLVMValueRef[2];
             arrayIndex[0] = zero;
            arrayIndex[1] = zero;
             PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayIndex);
            value = LLVMBuildGEP(builder, res, indexPointer, 2, "arrPtr");
        }
        else {
            value = LLVMBuildLoad(builder, res, /*varName:String*/varInTable.getName() + "_Rnum");
        }
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
