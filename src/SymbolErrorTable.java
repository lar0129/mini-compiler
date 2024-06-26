import symbol.Symbol;
import type.Type;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolErrorTable {
    private final Map<Integer, Integer> symbolErrors = new LinkedHashMap<>();
    // map(错误行号，错误类型)

    private boolean Error_Status = false;

    static String errorString[] =
            {null,"Var_Decl","Func_Decl","Var_Repeat","Func_Repeat",
            "Assign_UnMatch","Operator_UnMatch","Return_UnMatch","FuncCall_UnMatch",
            "Array_UnMatch","Var_Call","Assign_Func"};

    public SymbolErrorTable() {
    }

    public void addErrorTable(int errorLine,int errorType){
        symbolErrors.put(errorLine,errorType);
        Error_Status = true;
        System.err.printf("Error type %d at Line %d: %s\n",
                errorType,errorLine,errorString[errorType]);

    }

    public void setError_Status(boolean bool){
        Error_Status = bool;
    }

    public boolean isError_Status() {
        return Error_Status;
    }

    public boolean isErrorTableEmpty(){
        return symbolErrors.isEmpty();
    }

    public void printErrorTable(){
        for (Map.Entry<Integer, Integer> entry : symbolErrors.entrySet()){
            System.err.printf("Error type %d at Line %d: %s\n",
                    entry.getValue(),entry.getKey(),errorString[entry.getValue()]);
        }
    }
}
