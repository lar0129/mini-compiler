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

        List<? extends Token> tokens = sysYLexer.getAllTokens();
        if (!myListener.status) {
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                String text = token.getText();
                String type = SysYLexer.ruleNames[token.getType() - 1];
                if (type == "INTEGER_CONST"){
                    if (text.charAt(1)=='x' || text.charAt(1)=='X')
                        text = String.valueOf((Integer.parseInt(text,16)));
                    else if (text.charAt(0) == '0' && text.length()>1)
                        text = String.valueOf((Integer.parseInt(text,16)));
                }

                System.out.println(type + " " + text + " at Line " + token.getLine() + ".");
                //[token类型] [token文本] at Line [此token首个字符所在行的行号].
            }
        }
    }
}
