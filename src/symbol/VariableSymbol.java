package symbol;

import org.bytedeco.llvm.LLVM.*;
import type.Type;

public class VariableSymbol extends BaseSymbol {

  private LLVMValueRef Number;

  public VariableSymbol(String name, Type type) {
    super(name, type);
  }

  public LLVMValueRef getNumber() {
    return Number;
  }

  public void setNumber(LLVMValueRef number) {
    Number = number;
  }

}
