

import org.antlr.v4.runtime.ParserRuleContext;

public class PrintTreeListener  extends SysYParserBaseListener {
        String currentFunctionName = null;

        int deepIdx = 0;
        int temp = 0;

        String[] ruleNames = SysYParser.ruleNames;

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
//                deepIdx = ctx.depth();
//                System.out.println(deepIdx);
                temp = ctx.getAltNumber();
                System.out.println(temp);
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
//            String callee = ctx.ID().getText();
//            graph.addEdge(currentFunctionName, callee);
        }


}
