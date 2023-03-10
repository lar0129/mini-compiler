import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class myErrorListener extends BaseErrorListener {

    public static final myErrorListener INSTANCE = new myErrorListener();
    boolean status = false;

    public myErrorListener() {
        //System.out.println("Error type A at Line 3: Mysterious character \"~\".");
    }

    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        System.err.println("Error type A at Line " + line + ": " + msg);
        status = true;
    }

    public boolean getErrorStatus(){
        return status;
    }
}
