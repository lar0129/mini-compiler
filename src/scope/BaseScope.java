package scope;


import symbol.Symbol;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {
    private final Scope enclosingScope;
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();
    private String name;

    public BaseScope(String name, Scope enclosingScope) {
        this.name = name;
        this.enclosingScope = enclosingScope;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Scope getEnclosingScope() {
        return this.enclosingScope;
    }

    public Map<String, Symbol> getSymbols() {
        return this.symbols;
    }

    @Override
    public void define(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
//        System.out.println("define+ " + symbol.getName());
    }

    public void revokeDefine(Symbol symbol) {
//        symbols.remove(symbols.entrySet().iterator());
    }


    @Override
    public Symbol resolve(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
//            System.out.println("resolve* " + name);
            return symbol;
        }

        if (enclosingScope != null) {
            return enclosingScope.resolve(name);
        }

        System.err.println("Cannot find " + name);
        return null;
    }

    @Override
    public Symbol resolveInScope(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
           return symbol;
        }
        return null;
    }

    @Override
    public String toString() {
        return "name: " + name + "symbols: " + symbols.values();
    }
}
