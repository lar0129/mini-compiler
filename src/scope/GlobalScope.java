package scope;

import symbol.BasicTypeSymbol;

public class GlobalScope extends BaseScope {
    public GlobalScope(Scope enclosingScope) {
        super("GlobalScope", enclosingScope);
        define(new BasicTypeSymbol("int"));
        define(new BasicTypeSymbol("const int"));
        define(new BasicTypeSymbol("double"));
        define(new BasicTypeSymbol("void"));
    }
}