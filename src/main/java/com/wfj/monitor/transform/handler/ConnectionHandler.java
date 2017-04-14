package com.wfj.monitor.transform.handler;

import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class ConnectionHandler extends SqlMethodRewriteHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void doWeave(CtClass ctClass) {
        if (isConnection(ctClass)) {
            System.err.println("begin to wrap Connection");
            prepareMethodWeave(ctClass, "prepareStatement");
            prepareMethodWeave(ctClass, "prepareCall");
        } else if (this.getHandler() != null) {
            this.getHandler().doWeave(ctClass);
        }
    }

    private void prepareMethodWeave(CtClass ctClass, String methodName) {
        try {
            CtMethod[] ctMethods = ctClass.getDeclaredMethods(methodName);
            for (CtMethod ctMethod : ctMethods) {
                ctMethod.addLocalVariable("__methodRunTime", CtClass.longType);
                ctMethod.insertBefore("{__methodRunTime = -System.currentTimeMillis();}");
                ctMethod.insertAfter(
                        "__methodRunTime += System.currentTimeMillis();" +
                                "System.out.println(\"" + ctMethod
                                .getLongName() + " used \" + __methodRunTime + \"ms\");" +
                                "com.wfj.monitor.common.Printer.printAfterCreatePreparedStatement($0, $1, $_);"
                );
            }
        } catch (Exception e) {
            logger.warn("SKIPPED " + methodName + " in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

    protected boolean isConnection(CtClass ctClass) {
        return isImpl(ctClass, Connection.class) || isChild(ctClass, Connection.class);
    }
}
