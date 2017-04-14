package com.wfj.monitor.transform.handler;

import javassist.CtClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.LinkedList;

/**
 * <br>created at 17-4-13
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class DataSourceHandler extends SqlMethodRewriteHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final DataSourceHandler _HANDLER = new DataSourceHandler();

    static {
        _HANDLER.addLast(new DruidDataSourceHandler());
    }

    public static DataSourceHandler instance() {
        return _HANDLER;
    }

    public void doWeave(CtClass ctClass) {
        if (this.getHandler() != null) {
            this.getHandler().doWeave(ctClass);
        }
    }

    protected boolean isDataSource(CtClass ctClass) {
        return isImpl(ctClass, DataSource.class);
    }
}
