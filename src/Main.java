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

        List<? extends Token> tokens = sysYLexer.getAllTokens();
        for (int i=0;i<tokens.size();i++) {
            Token token = tokens.get(i);
            System.out.println(token.getType() + " " +token.getText() + " at Line " + token.getLine());
            //[token类型] [token文本] at Line [此token首个字符所在行的行号].
        }
    }
}
