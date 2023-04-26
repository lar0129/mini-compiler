package scope;
import symbol.Symbol;

import java.util.Map;

public interface Scope {
    public String getName();

    public void setName(String name);

    public Scope getEnclosingScope();

    // 符号表
    public Map<String, Symbol> getSymbols();

    public void define(Symbol symbol);

    // 往上解析符号
    public Symbol resolve(String name);

    public Symbol resolveInScope(String name);
}