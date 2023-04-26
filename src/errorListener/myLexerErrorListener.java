package errorListener;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class myLexerErrorListener extends BaseErrorListener {

    public static final myLexerErrorListener INSTANCE = new myLexerErrorListener();
    public static boolean status = false;

    public myLexerErrorListener() {
        status = false;
    }

    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        System.err.println("Error type A at Line " + line + ": " + msg + ".");
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
