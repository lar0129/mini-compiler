
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import symbol.BasicTypeSymbol;
import symbol.FunctionSymbol;
import symbol.Symbol;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMGlobalVisitor extends SysYParserBaseVisitor<LLVMValueRef> {

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
        return super.visitProgram(ctx);
    }

    @Override
    public LLVMValueRef visitCompUnit(SysYParser.CompUnitContext ctx) {
        return super.visitCompUnit(ctx);
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();

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

        return super.visitFuncDef(ctx);
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        return super.visitBlock(ctx);
    }

    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.RETURN() != null){
            LLVMValueRef retValue = visitExp(ctx.exp());
            //函数返回指令
            LLVMBuildRet(builder, /*result:LLVMValueRef*/retValue);
        }
        return super.visitStmt(ctx);
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

    @Override
    public LLVMValueRef visitFuncType(SysYParser.FuncTypeContext ctx) {
        return super.visitFuncType(ctx);
    }

    @Override
    public LLVMValueRef visitTerminal(TerminalNode node) {
        return super.visitTerminal(node);
    }
}
