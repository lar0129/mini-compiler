import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.*;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);

        sysYLexer.removeErrorListeners();
        myErrorListener myListener = new myErrorListener();
        sysYLexer.addErrorListener(myListener);
//
        List<? extends Token> tokens = sysYLexer.getAllTokens();
//        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
//        SysYParser sysYParser = new SysYParser(tokens);

        if (!myErrorListener.status) {
            for (Token token : tokens) {
                String text = token.getText();
                String type = SysYLexer.ruleNames[token.getType() - 1];
                if (type.equals("INTEGER_CONST")) {
                    if (text.length() > 2 && (text.charAt(1) == 'x' || text.charAt(1) == 'X')) {
                        text = String.valueOf((Integer.parseInt(text.substring(2), 16)));
                    } else if (text.charAt(0) == '0' && text.length() > 1) {
                        text = String.valueOf((Integer.parseInt(text.substring(1), 8)));
                    }
                }

                System.err.println(type + " " + text + " at Line " + token.getLine() + ".");
                // [token类型] [token文本] at Line [此token首个字符所在行的行号].
            }
        }
        myListener.setErrorStatus(false);
    }

}
//
//