package com.wfj.monitor.transform;

import com.wfj.monitor.transform.handler.HttpServletMethodRewriteHandler;
import com.wfj.monitor.transform.handler.SqlMethodRewriteHandler;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class MethodRewriteHandler {
    private static final MethodRewriteHandler _HANDLER = new MethodRewriteHandler();
    protected MethodRewriteHandler handler = null;

    static{
        _HANDLER.addLast(HttpServletMethodRewriteHandler.instance())
                .addLast(SqlMethodRewriteHandler.instance());
    }

    public static MethodRewriteHandler instance() {
        return _HANDLER;
    }

    public void doRewrite(CtClass ctClass) {
        assert ctClass != null;
        if (this.getHandler() != null) {
            this.getHandler().doRewrite(ctClass);
        }
    }

    protected boolean isChild(CtClass ctClass, Class<?> clazz) {
        CtClass superclass;
        try {
            superclass = ctClass.getSuperclass();
        } catch (NotFoundException e) {
            return false;
        }

        return !(superclass == null || Object.class.getName().equals(superclass.getName()))
                &&
                (clazz.getName().equals(superclass.getName()) || isChild(superclass, clazz));
    }

    protected static boolean isImpl(CtClass ctClass, Class<?> clazz) {
        CtClass[] interfaces;
        try {
            interfaces = ctClass.getInterfaces();
        } catch (NotFoundException e) {
            return false;
        }
        for (CtClass anInterface : interfaces) {
            if (clazz.getName().equals(anInterface.getName()) || isImpl(anInterface, clazz)) {
                return true;
            }
        }
        return false;
    }

    public MethodRewriteHandler getHandler() {
        return handler;
    }

    public void setHandler(MethodRewriteHandler handler) {
        this.handler = handler;
    }

    public MethodRewriteHandler addLast(MethodRewriteHandler handler) {
        this.setHandler(handler);
        return handler;
    }
}
