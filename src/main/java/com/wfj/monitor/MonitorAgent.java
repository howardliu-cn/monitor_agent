package com.wfj.monitor;

import com.wfj.monitor.conf.Constant;
import com.wfj.monitor.conf.EnvPropertyConfig;
import com.wfj.monitor.conf.SystemPropertyConfig;
import com.wfj.monitor.counter.SLACounter;
import com.wfj.monitor.dto.JavaInformations;
import com.wfj.monitor.handler.Health;
import com.wfj.monitor.handler.MonitorChecker;
import com.wfj.monitor.handler.factory.SLACountManager;
import com.wfj.monitor.handler.warpper.RequestWrapper;
import com.wfj.monitor.transform.MonitoringTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

import static com.wfj.monitor.common.Constant.IS_DEBUG;
import static com.wfj.monitor.common.Constant.SERVLET_CONTEXT;

/**
 * @Class Name MonitorAgent
 * @Author Jack
 * @Create In 2017年1月5日
 */
public class MonitorAgent {
    /**
     * 监控启动方法，完成初始化动作
     * 第一优先级
     * 使用参数形式，如：-javaagent:xxxx=xxx配置JVM
     *
     * @param args
     * @param inst
     * @Methods Name premain
     * @Create In 2017年1月5日 By Jack
     */
    public static void premain(String args, Instrumentation inst) {
        EnvPropertyConfig.init();
        if (args == null || args.isEmpty()) {
            SystemPropertyConfig.init();
        } else {
            SystemPropertyConfig.init(args);
        }
        if (IS_DEBUG) {
            return;
        }
        SLACounter.init();
        inst.addTransformer(new MonitoringTransformer());

        // TODO 暂时将监控代码放在下面，需要更优雅的写法
        JavaInformations javaInformations = JavaInformations.instance(SERVLET_CONTEXT, true);
        RequestWrapper.SINGLETON.initServletContext(SERVLET_CONTEXT);

        int port;
        if(javaInformations.getTomcatInformationsList().isEmpty()){
            port = Integer.valueOf(System.getProperty(Constant.SYSTEM_SEETING_SERVER_DEFAULT_SERVER_PORT, Constant.SYSTEM_SEETING_SERVER_DEFAULT_SERVER_PORT_VALUE));
        }else {
            port = Integer.valueOf(javaInformations.getTomcatInformationsList().get(0).getHttpPort());
        }
        com.wfj.monitor.common.Constant.SERVER_PORT = port;
        SLACountManager.init();
        Health hl = new MonitorChecker(port, SERVLET_CONTEXT);
        hl.startHealth("Active");
    }

    /**
     * 默认监控启动方法，完成初始化动作
     * 第二优先级
     * 使用参数形式，如：-javaagent:xxxx=xxx配置JVM
     *
     * @param args void
     * @Methods Name premain
     * @Create In 2017年1月5日 By Jack
     */
    public static void premain(String args) {
    }

    /**
     * 动态代理注入启动方法，完成初始化动作
     * 需配合线程类型主程序使用
     * 使用非参数形式，如：-javaagent:xxxx=xxx配置JVM
     *
     * @param agentArguments
     * @param instrumentation
     * @throws UnmodifiableClassException void
     * @Methods Name agentmain
     * @Create In 2017年1月5日 By Jack
     */
    public static void agentmain(String agentArguments, Instrumentation instrumentation)
            throws UnmodifiableClassException {
    }
}
