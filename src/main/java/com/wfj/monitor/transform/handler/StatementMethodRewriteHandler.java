package com.wfj.monitor.transform.handler;

import com.wfj.monitor.transform.MethodRewriteHandler;
import javassist.CtClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class StatementMethodRewriteHandler extends SqlMethodRewriteHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void doRewrite(CtClass ctClass) {
        if (isStatement(ctClass)) {
            System.err.println("begin to wrap Statement");
//        execute
//        executeQuery
//        executeUpdate
//        executeBatch
//        executeLargeBatch
//        executeLargeUpdate
        } else if (this.getHandler() != null) {
            this.getHandler().doRewrite(ctClass);
        }
    }

    protected boolean isStatement(CtClass ctClass) {
        return MethodRewriteHandler.isImpl(ctClass, Statement.class) || isChild(ctClass, Statement.class);
    }
}
