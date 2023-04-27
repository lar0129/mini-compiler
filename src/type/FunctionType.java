package type;
import java.util.ArrayList;
import scope.*;
import symbol.*;

public class FunctionType implements Type {
    Type retTy;
    // 返回类型

    ArrayList<Type> paramsType;
    // 参数数量

    public FunctionType() {
    }

    public FunctionType(Type retTy, ArrayList<Type> paramsType) {
        this.retTy = retTy;
        this.paramsType = paramsType;
    }

    public String retToString() {
        return retTy.toString();
    }


}