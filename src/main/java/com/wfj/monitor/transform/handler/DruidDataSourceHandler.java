package com.wfj.monitor.transform.handler;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <br>created at 17-4-13
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class DruidDataSourceHandler extends DataSourceHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void doWeave(CtClass ctClass) {
        if (isDruidDataSource(ctClass)) {
            System.err.println("begin to wrap DruidDataSource");
            doWeaveInit(ctClass);
        } else if (this.getHandler() != null) {
            this.getHandler().doWeave(ctClass);
        }
    }

    public void doWeaveInit(CtClass ctClass) {
        try {
            CtMethod ctMethod = ctClass.getDeclaredMethod("init");
            ctMethod.insertAfter(
                    "com.wfj.monitor.handler.wrapper.JdbcWrapper.SINGLETON.fillDataSourceInfo($0);" +
                            "com.wfj.monitor.handler.wrapper.JdbcWrapper.registerCommonDataSource($0.getClass().getName() + \"@\" + System.identityHashCode($0), $0);"
            );

        } catch (NotFoundException e) {
            logger.info("not found init method in " + ctClass.getName());
        } catch (Exception e) {
            logger.warn("SKIPPED init() in " + ctClass.getName() + ", the reason is " + e.getMessage());
        }
    }

    private boolean isDruidDataSource(CtClass ctClass) {
        return isDataSource(ctClass)
                &&
                (
                        "com.alibaba.druid.pool.DruidDataSource".equals(ctClass.getName())
                                ||
                                isChild(ctClass, "com.alibaba.druid.pool.DruidDataSource")
                );
    }
}
