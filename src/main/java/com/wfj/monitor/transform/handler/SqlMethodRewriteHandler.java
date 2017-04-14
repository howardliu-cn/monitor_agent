package com.wfj.monitor.transform.handler;

import com.wfj.monitor.transform.MethodRewriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class SqlMethodRewriteHandler extends MethodRewriteHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final SqlMethodRewriteHandler _HANDLER = new SqlMethodRewriteHandler();

    static {
        _HANDLER
                .addLast(DataSourceHandler.instance())
                .addLast(new ConnectionHandler())
                .addLast(new PreparedStatementHandler())
                .addLast(new StatementHandler());
    }

    public static SqlMethodRewriteHandler instance() {
        return _HANDLER;
    }
}
