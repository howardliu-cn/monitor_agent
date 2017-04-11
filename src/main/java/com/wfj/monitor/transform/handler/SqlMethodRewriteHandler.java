package com.wfj.monitor.transform.handler;

import com.wfj.monitor.transform.MethodRewriteHandler;
import javassist.CtClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class SqlMethodRewriteHandler extends MethodRewriteHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final SqlMethodRewriteHandler THIS_HANDLER = new SqlMethodRewriteHandler();

    public static SqlMethodRewriteHandler instance() {
        THIS_HANDLER
                .addLast(new ConnectionMethodRewriteHandler())
                .addLast(new PreparedStatementMethodRewriteHandler())
                .addLast(new StatementMethodRewriteHandler());
        return THIS_HANDLER;
    }

    @Override
    public void doRewrite(CtClass ctClass) {
        if (this.getHandler() != null) {
            this.getHandler().doRewrite(ctClass);
        }
    }
}
