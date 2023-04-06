

import org.antlr.v4.runtime.ParserRuleContext;

public class PrintTreeListener  extends SysYParserBaseListener {
        String currentFunctionName = null;

        int tempIdx = 0;

        String[] ruleNames = SysYParser.ruleNames;

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
                tempIdx = ctx.depth();
                System.out.println(tempIdx);
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
//            String callee = ctx.ID().getText();
//            graph.addEdge(currentFunctionName, callee);
        }


}
