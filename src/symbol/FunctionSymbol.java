package symbol;

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

  public void setFunctionType(Type retTy, ArrayList<Type> paramsType){
    functionType = new FunctionType(retTy,paramsType);
  }

  public Type getType() {
    return functionType.getRetTy();
  }
  public FunctionType getFunctionType() {
    return functionType;
  }

}