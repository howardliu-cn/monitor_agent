package com.wfj.monitor.transform.handler;

import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class PreparedStatementHandler extends SqlMethodRewriteHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void doWeave(CtClass ctClass) {
        if (isPreparedStatement(ctClass)) {
            System.err.println("begin to wrap PreparedStatement");
            doWeave(ctClass, "execute");
            doWeave(ctClass, "executeQuery");
            doWeave(ctClass, "executeUpdate");
            doWeave(ctClass, "executeLargeUpdate");
            doWeaveClose(ctClass);
        } else if (this.getHandler() != null) {
            this.getHandler().doWeave(ctClass);
        }
    }

    private void doWeave(CtClass ctClass, String methodName) {
        try {
            CtMethod[] ctMethods = ctClass.getDeclaredMethods(methodName);
            for (CtMethod ctMethod : ctMethods) {
                ctMethod.addLocalVariable("__methodRunTime", CtClass.longType);
                ctMethod.insertBefore("{__methodRunTime = -System.currentTimeMillis();}");
                ctMethod.insertAfter(
                        "__methodRunTime += System.currentTimeMillis();" +
                                "System.err.println(\"" + ctMethod
                                .getLongName() + " used \" + __methodRunTime + \"ms\");" +
                                "com.wfj.monitor.common.Printer.printAfterExecuteInPreparedStatement($0);"
                );
            }
        } catch (Exception e) {
            logger.warn("SKIPPED " + methodName + " in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

    private void doWeaveClose(CtClass ctClass) {
        try {
            CtMethod[] ctMethods = ctClass.getDeclaredMethods("close");
            for (CtMethod ctMethod : ctMethods) {
                ctMethod.insertAfter("com.wfj.monitor.common.Printer.printAfterPreparedStatementClose($0);");
            }
        } catch (Exception e) {
            logger.warn("SKIPPED close in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

    protected boolean isPreparedStatement(CtClass ctClass) {
        return isImpl(ctClass, PreparedStatement.class) || isChild(ctClass, PreparedStatement.class);
    }
}
