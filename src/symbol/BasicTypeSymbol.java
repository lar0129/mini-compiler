package symbol;

import type.Type;

// 符号化类型
public class BasicTypeSymbol extends BaseSymbol implements Type {
  public BasicTypeSymbol(String name) {
    super(name, null);
  }

  @Override
  public String toString() {
    return name;
  }
}
