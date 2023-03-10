import org.antlr.v4.runtime.BaseErrorListener;

public class myErrorListener extends BaseErrorListener {
    public myErrorListener() {
        System.out.println("Error type A at Line 3: Mysterious character \"~\".");
    }


}
