// Generated from java-escape by ANTLR 4.11.1
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SysYParser0}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SysYParser0Visitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SysYParser0#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExp(SysYParser0.ExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser0#cond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCond(SysYParser0.CondContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser0#lVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLVal(SysYParser0.LValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser0#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(SysYParser0.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser0#unaryOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOp(SysYParser0.UnaryOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser0#funcRParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncRParams(SysYParser0.FuncRParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser0#param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam(SysYParser0.ParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser0#constExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstExp(SysYParser0.ConstExpContext ctx);
}