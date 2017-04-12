package com.wfj.monitor.transform.handler;

import com.wfj.monitor.transform.MethodRewriteHandler;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
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
    private ClassPool classPool;

    public static HttpServletMethodRewriteHandler instance() {
        return THIS_HANDLER;
    }

    public void doRewrite(CtClass ctClass) {
        if (isHttpServlet(ctClass)) {
            classPool = ClassPool.getDefault();
            logger.debug("begin to wrapping HttpServlet");
            doRewriteInit(ctClass);
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

    private void doRewriteInit(CtClass ctClass) {
        try {
            CtClass[] params = {classPool.get(FilterConfig.class.getName())};
            CtMethod ctMethod = ctClass.getDeclaredMethod("init", params);
            ctMethod.insertBefore(
                    "if(com.wfj.monitor.common.Constant.servletContext == null) {com.wfj.monitor.common.Constant.servletContext = $1;}"
            );
        } catch (NotFoundException ignored) {
        } catch (Exception e) {
            logger.warn("SKIPPED init(javax.servlet.FilterConfig) in " + ctClass.getName() +
                    ", the reason is " + e.getMessage());
        }
    }

    private void doRewrite(CtClass ctClass, String methodName) {
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
        } catch (Exception e) {
            logger.warn("SKIPPED " + methodName + " in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

}
