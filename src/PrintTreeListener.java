

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class PrintTreeListener  extends SysYParserBaseListener {
        String currentFunctionName = null;

        String[] ruleNames = SysYParser.ruleNames;

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
                int deepIdx = ctx.depth();
//                System.out.println(deepIdx);
                int ruleIdx = ctx.getRuleIndex();
//                System.out.println(ruleIdx);
                String temp = "";
                for (int i=1;i<deepIdx;i++){
                        temp+="  ";
                }
                temp+=ruleNames[ruleIdx];
                System.out.println(temp);
        }

        @Override public void visitTerminal(TerminalNode node) {

        }

        @Override public void visitErrorNode(ErrorNode node) {

        }


}
