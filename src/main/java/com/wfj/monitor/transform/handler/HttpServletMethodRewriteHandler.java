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
    private static final HttpServletMethodRewriteHandler SERVLET_HANDLER = new HttpServletMethodRewriteHandler();
    private ClassPool classPool;

    public static HttpServletMethodRewriteHandler instance() {
        return SERVLET_HANDLER;
    }

    public void doWeave(CtClass ctClass) {
        if (isHttpServlet(ctClass)) {
            classPool = ClassPool.getDefault();
            logger.debug("begin to wrapping HttpServlet");
            doWeaveInit(ctClass);
            doWeave(ctClass, "doHead");
            doWeave(ctClass, "doGet");
            doWeave(ctClass, "doPost");
            doWeave(ctClass, "doPut");
            doWeave(ctClass, "doDelete");
            doWeave(ctClass, "doOptions");
            doWeave(ctClass, "doTrace");
            logger.debug("ended wrap HttpServlet");
        } else if (this.getHandler() != null) {
            this.getHandler().doWeave(ctClass);
        }
    }

    private boolean isHttpServlet(CtClass ctClass) {
        return isChild(ctClass, HttpServlet.class);
    }

    private void doWeaveInit(CtClass ctClass) {
        try {
            CtMethod ctMethod = ctClass.getDeclaredMethod("init");
            ctMethod.insertAfter(
                    "if(com.wfj.monitor.common.Constant.SERVLET_CONTEXT == null) {" +
                            "com.wfj.monitor.common.Constant.SERVLET_CONTEXT = $0.getServletContext();" +
                            "com.wfj.monitor.counter.MonitorStarter.run();" +
                            "}"
            );
        } catch (NotFoundException ignored) {
            logger.info("not found init method in " + ctClass.getName());
        } catch (Exception e) {
            logger.warn("SKIPPED init() in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

    private void doWeave(CtClass ctClass, String methodName) {
        try {
            CtClass[] params = {
                    classPool.get(HttpServletRequest.class.getName()),
                    classPool.get(HttpServletResponse.class.getName())
            };
            CtMethod ctMethod = ctClass.getDeclaredMethod(methodName, params);
            ctMethod.insertBefore(
                    "com.wfj.monitor.counter.RequestCounter.ThreadRequest.begin(Thread.currentThread().getId(), $1, $2);"
            );
            ctMethod.addCatch(
                    "com.wfj.monitor.counter.RequestCounter.ThreadRequest.catchBlock(Thread.currentThread().getId(), $1, $2, $e);" +
                            "throw $e;",
                    classPool.get(Throwable.class.getName())
            );
            ctMethod.insertAfter(
                    "com.wfj.monitor.counter.RequestCounter.ThreadRequest.end(Thread.currentThread().getId(), $1, $2);"
            );
        } catch (NotFoundException ignored) {
            logger.info("not found " + methodName + " method in " + ctClass.getName());
        } catch (Exception e) {
            logger.warn("SKIPPED " + methodName + " in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }
}
