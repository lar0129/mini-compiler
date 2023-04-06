import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class myParserErrorListener extends BaseErrorListener {

    public static final myParserErrorListener INSTANCE = new myParserErrorListener();
    public static boolean status = false;

    public myParserErrorListener() {
        //System.out.println("Error type A at Line 3: Mysterious character \"~\".");
        status = false;
    }

    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        System.err.println("Error type B at Line " + line + ": " + msg + ".");
        status = true;
//        System.out.println(status);
    }

    public boolean getErrorStatus(){
        return status;
    }

    public void setErrorStatus(boolean set){
        status = set;
    }
}
