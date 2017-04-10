package com.wfj.monitor.transform;

import com.wfj.monitor.conf.EnvPropertyConfig;
import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * <br>created at 17-4-5
 *
 * @author liuxh
 * @since 1.0.0
 */
public class MonitoringTransformer implements ClassFileTransformer {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ClassPool classPool;

    public MonitoringTransformer() {
        classPool = ClassPool.getDefault();
        try {
            classPool.appendPathList(System.getProperty("java.class.path"));
            classPool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));
        } catch (NotFoundException e) {
            logger.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001000"), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (loader == null
                || ApmFilter.isNotNeedInjectClassLoader(loader.getClass().getName())
                || ApmFilter.isNotNeedInject(className)) {
            logger.debug(className + "is not excluded, SKIPPED!");
            return classfileBuffer;
        }

        if (className.contains("Proxy") || className.contains("CGLIB")) {
            logger.debug(className + "is not proxy, SKIPPED!");
            return classfileBuffer;
        }

        String fullyQualifiedClassName = className.replace("/", ".");
        classPool.appendClassPath(new ByteArrayClassPath(fullyQualifiedClassName, classfileBuffer));

        CtClass ctClass;
        try {
            try {
                ctClass = classPool.get(fullyQualifiedClassName);
            } catch (NotFoundException e) {
                ctClass = classPool.makeClass(fullyQualifiedClassName);
            }

            //冻结的类，不需要处理
            if (ctClass.isFrozen()) {
                logger.debug(className + " is frozen, SKIPPED!");
                return classfileBuffer;
            }

            if (ctClass.isModified()) {
                logger.debug(className + " is modified, SKIPPED!");
                return classfileBuffer;
            }

            //类型、枚举、接口、注解等不需要处理的类
            if (ctClass.isPrimitive()
                    || ctClass.isArray()
                    || ctClass.isAnnotation()
                    || ctClass.isEnum()
                    || ctClass.isInterface()) {
                logger.debug(className + "is not a class, SKIPPED!");
                return classfileBuffer;
            }

            doHttpServletProxy(ctClass);
            doSqlProxy(ctClass);

            return ctClass.toBytecode();
        } catch (Exception e) {
            logger.error("className: " + className, e);
            logger.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001002"), e);
        }
        return classfileBuffer;
    }

    private void doHttpServletProxy(CtClass ctClass) {
        if (isHttpServlet(ctClass)) {
            logger.debug("begin to wrap HttpServlet");
            doMethodProxy(ctClass, "doHead");
            doMethodProxy(ctClass, "doGet");
            doMethodProxy(ctClass, "doPost");
            doMethodProxy(ctClass, "doPut");
            doMethodProxy(ctClass, "doDelete");
            doMethodProxy(ctClass, "doOptions");
            doMethodProxy(ctClass, "doTrace");
            logger.debug("ended wrap HttpServlet");
        }
    }

    private boolean isHttpServlet(CtClass ctClass) {
        return isChild(ctClass, HttpServlet.class);
    }

    private void doMethodProxy(CtClass ctClass, String methodName) {
        try {
            CtClass[] params = {
                    classPool.get(HttpServletRequest.class.getName()),
                    classPool.get(HttpServletResponse.class.getName())
            };
            CtMethod ctMethod = ctClass.getDeclaredMethod("methodName", params);
            ctMethod.addLocalVariable("__methodRunTime", CtClass.longType);
            ctMethod.insertBefore("__methodRunTime = -System.currentTimeMillis();");
            ctMethod.insertAfter(
                    "__methodRunTime += System.currentTimeMillis();" +
                            "System.out.println(\"" + ctMethod.getName() + " used \" + __methodRunTime + \"ms\");"
            );
        } catch (Exception e) {
            logger.debug("SKIPPED " + methodName + " in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

    private void doSqlProxy(CtClass ctClass) {
        if (isConnection(ctClass)) {
            sqlConnectionProxy(ctClass);
        } else if (isPreparedStatement(ctClass)) {
            sqlPreparedStatementProxy(ctClass);
        } else if (isStatement(ctClass)) {
            sqlStatementProxy(ctClass);
        }
    }

    private boolean isConnection(CtClass ctClass) {
        return isImpl(ctClass, Connection.class) || isChild(ctClass, Connection.class);
    }

    private void sqlConnectionProxy(CtClass ctClass) {
        prepareMethodProxy(ctClass, "prepareStatement");
        prepareMethodProxy(ctClass, "prepareCall");
    }

    private void prepareMethodProxy(CtClass ctClass, String methodName) {
        try {
            CtMethod[] ctMethods = ctClass.getDeclaredMethods(methodName);
            for (CtMethod ctMethod : ctMethods) {
                ctMethod.addLocalVariable("__methodRunTime", CtClass.longType);
                ctMethod.insertBefore("{__methodRunTime = -System.currentTimeMillis();}");
                ctMethod.insertAfter(
                        "__methodRunTime += System.currentTimeMillis();" +
                                "System.out.println(\"" + ctMethod
                                .getLongName() + " used \" + __methodRunTime + \"ms\");" +
                                "com.wfj.monitor.common.Counter.printAfterCreatePreparedStatement($0, $1, $_);"
                );
            }
        } catch (Exception e) {
            logger.warn("SKIPPED " + methodName + " in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

    private boolean isPreparedStatement(CtClass ctClass) {
        return isImpl(ctClass, PreparedStatement.class) || isChild(ctClass, PreparedStatement.class);
    }

    private void sqlPreparedStatementProxy(CtClass ctClass) {
        executeProxyInPreparedStatement(ctClass, "execute");
        executeProxyInPreparedStatement(ctClass, "executeQuery");
        executeProxyInPreparedStatement(ctClass, "executeUpdate");
        executeProxyInPreparedStatement(ctClass, "executeLargeUpdate");
        cleanPreparedStatement(ctClass);
    }

    private void executeProxyInPreparedStatement(CtClass ctClass, String methodName) {
        try {
            CtMethod[] ctMethods = ctClass.getDeclaredMethods(methodName);
            for (CtMethod ctMethod : ctMethods) {
                ctMethod.addLocalVariable("__methodRunTime", CtClass.longType);
                ctMethod.insertBefore("{__methodRunTime = -System.currentTimeMillis();}");
                ctMethod.insertAfter(
                        "__methodRunTime += System.currentTimeMillis();" +
                                "System.err.println(\"" + ctMethod
                                .getLongName() + " used \" + __methodRunTime + \"ms\");" +
                                "com.wfj.monitor.common.Counter.printAfterExecuteInPreparedStatement($0);"
                );
            }
        } catch (Exception e) {
            logger.warn("SKIPPED " + methodName + " in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

    private void cleanPreparedStatement(CtClass ctClass) {
        try {
            CtMethod[] ctMethods = ctClass.getDeclaredMethods("close");
            for (CtMethod ctMethod : ctMethods) {
                ctMethod.insertAfter("com.wfj.monitor.common.Counter.printAfterPreparedStatementClose($0);");
            }
        } catch (Exception e) {
            logger.warn("SKIPPED close in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

    private boolean isStatement(CtClass ctClass) {
        return isImpl(ctClass, Statement.class) || isChild(ctClass, Statement.class);
    }

    private void sqlStatementProxy(CtClass ctClass) {
//        execute
//        executeQuery
//        executeUpdate
//        executeBatch
//        executeLargeBatch
//        executeLargeUpdate
    }

    private boolean isChild(CtClass ctClass, Class<?> clazz) {
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

    private static boolean isImpl(CtClass ctClass, Class<?> clazz) {
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
}
