package com.wfj.monitor.transform.handler;

import com.wfj.monitor.transform.MethodRewriteHandler;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class HttpServletMethodRewriteHandler extends MethodRewriteHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final HttpServletMethodRewriteHandler THIS_HANDLER = new HttpServletMethodRewriteHandler();

    public static HttpServletMethodRewriteHandler instance() {
        return THIS_HANDLER;
    }

    public void doRewrite(CtClass ctClass) {
        if (isHttpServlet(ctClass)) {
            System.err.println("begin to wrap : " + ctClass.getName());
            logger.debug("begin to wrap HttpServlet");
            doRewrite(ctClass, "doHead");
            doRewrite(ctClass, "doGet");
            doRewrite(ctClass, "doPost");
            doRewrite(ctClass, "doPut");
            doRewrite(ctClass, "doDelete");
            doRewrite(ctClass, "doOptions");
            doRewrite(ctClass, "doTrace");
            logger.debug("ended wrap HttpServlet");
        } else if (this.getHandler() != null) {
            this.getHandler().doRewrite(ctClass);
        }
    }

    private boolean isHttpServlet(CtClass ctClass) {
        return isChild(ctClass, HttpServlet.class);
    }

    private void doRewrite(CtClass ctClass, String methodName) {
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass[] params = {
                    classPool.get(HttpServletRequest.class.getName()),
                    classPool.get(HttpServletResponse.class.getName())
            };
            CtMethod ctMethod = ctClass.getDeclaredMethod(methodName, params);
            ctMethod.addLocalVariable("__methodRunTime", CtClass.longType);
            ctMethod.addLocalVariable("__methodThrowable", classPool.get(Throwable.class.getName()));
            ctMethod.insertBefore(
                    "__methodRunTime = -System.currentTimeMillis();"
            );
            ctMethod.addCatch(
                    "com.wfj.monitor.common.Printer.add(Thread.currentThread().getId(), $e);" +
                            "throw $e;",
                    classPool.get(Throwable.class.getName())
            );
            ctMethod.insertAfter(
                    "__methodRunTime += System.currentTimeMillis();" +
                            "__methodThrowable = com.wfj.monitor.common.Printer.getAndRemove(Thread.currentThread().getId());" +
                            "System.out.println(\"" + ctMethod.getName() + " used \" + __methodRunTime + \"ms\");" +
                            "com.wfj.monitor.common.Printer.counter($1, $2, __methodRunTime, __methodThrowable);"
            );

        } catch (NotFoundException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("SKIPPED " + methodName + " in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

}
