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

    static {
        _HANDLER.addLast(HttpServletMethodRewriteHandler.instance())
                .addLast(SqlMethodRewriteHandler.instance())
        ;
    }

    public static MethodRewriteHandler instance() {
        return _HANDLER;
    }

    public void doWeave(CtClass ctClass) {
        assert ctClass != null;
        if (this.getHandler() != null) {
            this.getHandler().doWeave(ctClass);
        }
    }

    protected static boolean isChild(CtClass ctClass, Class<?> clazz) {
        return isChild(ctClass, clazz.getName());
    }

    protected static boolean isChild(CtClass ctClass, String className) {
        if (ctClass.getName().equals(className)) {
            return true;
        }
        CtClass superclass;
        try {
            superclass = ctClass.getSuperclass();
        } catch (NotFoundException e) {
            return false;
        }

        return !(superclass == null || "java.lang.Object".equals(superclass.getName()))
                &&
                (className.equals(superclass.getName()) || isChild(superclass, className));
    }

    protected static boolean isImpl(CtClass ctClass, Class<?> clazz) {
        return isImpl(ctClass, clazz.getName());
    }

    protected static boolean isImpl(CtClass ctClass, String interfaceName) {
        if (ctClass.getName().equals(interfaceName)) {
            return true;
        }
        CtClass[] interfaces;
        try {
            interfaces = ctClass.getInterfaces();
        } catch (NotFoundException e) {
            return false;
        }
        for (CtClass anInterface : interfaces) {
            if (interfaceName.equals(anInterface.getName()) || isImpl(anInterface, interfaceName)) {
                return true;
            }
        }
        try {
            CtClass superclass = ctClass.getSuperclass();
            return !(superclass == null || "java.lang.Object".equals(superclass.getName()))
                    &&
                    isImpl(superclass, interfaceName);
        } catch (NotFoundException ignored) {
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
        MethodRewriteHandler _handler = this.getHandler();
        MethodRewriteHandler tail = this;
        while (_handler != null) {
            tail = _handler;
            _handler = _handler.getHandler();
        }
        tail.setHandler(handler);
        return handler;
    }
}
