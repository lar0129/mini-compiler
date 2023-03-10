import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class myErrorListener extends BaseErrorListener {
    public myErrorListener() {
        //System.out.println("Error type A at Line 3: Mysterious character \"~\".");
    }

    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        System.out.println("Error type A at Line " + line + ": " + msg);
    }

}
