

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class PrintTreeListener  extends SysYParserBaseListener {
        String currentFunctionName = null;
        String[] ruleNames = SysYParser.ruleNames;

//      String[] vocabulary{
//                null, "CONST", "INT", "VOID", "IF", "ELSE", "WHILE", "BREAK", "CONTINUE",
//                        "RETURN", "PLUS", "MINUS", "MUL", "DIV", "MOD", "ASSIGN", "EQ", "NEQ",
//                        "LT", "GT", "LE", "GE", "NOT", "AND", "OR", "L_PAREN", "R_PAREN", "L_BRACE",
//                        "R_BRACE", "L_BRACKT", "R_BRACKT", "COMMA", "SEMICOLON", "IDENT", "INTEGER_CONST",
//                        "WS", "LINE_COMMENT", "MULTILINE_COMMENT"
//        };

        String[] lexerColor = {
                null, "orange", "orange", "orange", "orange", "orange", "orange", "orange", "orange",
                        "orange", "blue", "blue", "blue", "blue", "blue", "blue", "blue", "blue",
                        "blue", "blue", "blue", "blue", "blue", "blue", "blue", "", "", "",
                        "", "", "", "", "", "red", "green",
                        "", "", ""
        };

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
                int deepIdx = ctx.depth();
                int ruleIdx = ctx.getRuleIndex();
                String temp = "";
                for (int i=1;i<deepIdx;i++){
                        temp+="  ";
                }

                temp += ruleNames[ruleIdx].substring(0, 1).toUpperCase() + ruleNames[ruleIdx].substring(1);
                ;
                System.out.println(temp);
        }

        @Override public void visitTerminal(TerminalNode node) {
                int deepIdx = ((RuleContext)node.getParent()).depth() + 1;
                String terText = node.getSymbol().getText();
                int terTypeIdx = node.getSymbol().getType();
                String terType = SysYParser.VOCABULARY.getSymbolicName(terTypeIdx);
                // 转换进制
                if(terType.equals("INTEGER_CONST")){
                        terText = Main.HEXtoTEN(terText);
                }

                // 排除特殊
                String terTypeColor = "";
                if(terTypeIdx!=-1) {
                        terTypeColor = lexerColor[terTypeIdx];
                }
                if (!terTypeColor.equals("") && terTypeIdx!=-1) {
                        String temp = "";
                        for (int i=1;i<deepIdx;i++){
                                temp+="  ";
                        }

                        System.out.println(temp + terText + " " + terType + "[" + terTypeColor + "]");
                }
        }

        @Override public void visitErrorNode(ErrorNode node) {

        }


}
