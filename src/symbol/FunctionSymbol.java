package symbol;

import scope.BaseScope;
import scope.Scope;

public class FunctionSymbol extends BaseScope implements Symbol {
  public FunctionSymbol(String name, Scope enclosingScope) {
    super(name, enclosingScope);
  }
}