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

    public void addParams(Type type){
        paramsType.add(type);
    }

    public Type getRetTy() {
        return retTy;
    }

    @Override
    public String toString() {
        String ret = "";
        for(int i=0;i<paramsType.size();i++){
            ret = ret + paramsType.get(i).toString();
        }
        return retTy + "(" + ret + ")";
    }


}