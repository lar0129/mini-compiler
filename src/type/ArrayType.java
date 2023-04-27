package type;
import org.antlr.v4.runtime.ParserRuleContext;
import scope.*;
import symbol.*;
import java.util.ArrayList;

public class ArrayType implements Type {
    public Type elementType;
    // element类型

    public int elementNums;
    // element数量

    public int arrayDimension = 0;

    public void setElementType(Type elementType) {
        this.elementType = elementType;
    }

    public void setArrayDimension(int arrayDimension) {
        this.arrayDimension = arrayDimension;
    }

    public void setElementNums(int elementNums) {
        this.elementNums = elementNums;
    }
}

