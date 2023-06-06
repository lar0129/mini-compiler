package symbol;

import org.bytedeco.llvm.LLVM.LLVMValueRef;
import scope.BaseScope;
import scope.Scope;
import type.FunctionType;
import type.Type;

import java.util.ArrayList;

public class FunctionSymbol extends BaseScope implements Symbol {
  public FunctionSymbol(String name, Scope enclosingScope) {
    super(name, enclosingScope);
  }

  //因无法继承BaseScope，需单独存储Type
  private FunctionType functionType = null;
  private LLVMValueRef funcLLVM;

  public LLVMValueRef getFuncRef() {
    return funcLLVM;
  }

  public void setFuncRef(LLVMValueRef funcLLVM) {
    this.funcLLVM = funcLLVM;
  }

  public void setFunctionType(Type retTy, ArrayList<Type> paramsType){
    functionType = new FunctionType(retTy,paramsType);
  }

  public FunctionType getType() {
    return functionType;
  }

}