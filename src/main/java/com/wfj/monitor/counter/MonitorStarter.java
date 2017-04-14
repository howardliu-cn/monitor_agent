package com.wfj.monitor.counter;

import com.wfj.monitor.conf.Constant;
import com.wfj.monitor.dto.JavaInformations;
import com.wfj.monitor.handler.Health;
import com.wfj.monitor.handler.MonitorChecker;
import com.wfj.monitor.handler.factory.SLACountManager;
import com.wfj.monitor.handler.wrapper.RequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.wfj.monitor.common.Constant.SERVLET_CONTEXT;

/**
 * <br>created at 17-4-12
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class MonitorStarter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static boolean isStarted = false;

    public static void run() {
        if (!isStarted) {
            Thread t = new Thread(new MonitorStarterRunner());
            t.setDaemon(true);
            t.setName("monitor-starter-thread");
            t.run();
            isStarted = true;
        }
    }

    static class MonitorStarterRunner implements Runnable {
        @Override
        public void run() {
            if (com.wfj.monitor.common.Constant.IS_DEBUG) {
                return;
            }
            // TODO 暂时将监控代码放在下面，需要更优雅的写法
            JavaInformations javaInformations = JavaInformations.instance(SERVLET_CONTEXT, true);
            RequestWrapper.SINGLETON.initServletContext(SERVLET_CONTEXT);

            int port;
            if (javaInformations.getTomcatInformationsList().isEmpty()) {
                port = Integer.valueOf(System.getProperty(Constant.SYSTEM_SEETING_SERVER_DEFAULT_SERVER_PORT,
                        Constant.SYSTEM_SEETING_SERVER_DEFAULT_SERVER_PORT_VALUE));
            } else {
                port = Integer.valueOf(javaInformations.getTomcatInformationsList().get(0).getHttpPort());
            }

            com.wfj.monitor.common.Constant.SERVER_PORT = port;

            SLACountManager.init();

            Health hl = new MonitorChecker(port, SERVLET_CONTEXT);
            hl.startHealth("Active");
        }
    }
}
