

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class PrintTreeListener  extends SysYParserBaseListener {
        String currentFunctionName = null;

        String[] ruleNames = SysYParser.ruleNames;

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
                int deepIdx = ctx.depth();
                int ruleIdx = ctx.getRuleIndex();
                String temp = "";
                for (int i=1;i<deepIdx;i++){
                        temp+="  ";
                }
                temp+=ruleNames[ruleIdx];
                System.out.println(temp);
        }

        @Override public void visitTerminal(TerminalNode node) {
                String terText = node.getSymbol().getText();
                String terType = SysYParser.VOCABULARY.getSymbolicName(node.getSymbol().getType());
                System.out.println(terText);
                System.out.println(terType);
        }

        @Override public void visitErrorNode(ErrorNode node) {

        }


}
