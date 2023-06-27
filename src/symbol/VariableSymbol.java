package symbol;

import org.bytedeco.llvm.LLVM.*;
import type.Type;

import static org.bytedeco.llvm.global.LLVM.LLVMInt32Type;

public class VariableSymbol extends BaseSymbol {

  private LLVMValueRef Number;

  private LLVMTypeRef intType = LLVMInt32Type();;

  public VariableSymbol(String name, Type type) {
    super(name, type);
  }

  public LLVMValueRef getNumber() {
    return Number;
  }

  public void setNumber(LLVMValueRef number) {
    Number = number;
  }

  public LLVMTypeRef getIntType() {
    return intType;
  }

  public void setIntType(LLVMTypeRef type) {
    intType = type;
  }

}
