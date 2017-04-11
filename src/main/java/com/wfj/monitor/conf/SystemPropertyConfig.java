/**
 * @Probject Name: netty
 * @Path: com.wfj.netty.confSystemPropertyConfig.java
 * @Create By Jack
 * @Create In 2015年8月25日 上午5:12:24
 * TODO
 */
package com.wfj.monitor.conf;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Class Name SystemPropertyConfig
 * @Author Jack
 * @Create In 2015年8月25日
 */
public class SystemPropertyConfig{

	private static Logger log = LoggerFactory.getLogger(SystemPropertyConfig.class);

	private static Properties pc = null;

	private static final String PROPERTY_CONTEXT_PATH_CUSTOMER = "/wfj-netty.properties";

	public static void init(){
		if(pc != null){
			return ;
		}


		try {
			InputStream in = SystemPropertyConfig.class.getResourceAsStream(PROPERTY_CONTEXT_PATH_CUSTOMER);
			pc = new Properties();
			pc.load(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("Customer Server Resource file did not find, please check Netty-Server-WFJ.jar exist! Details: ");
			log.error(e.getMessage());
		}
	}

	/**
	 * 返回指定属性的值
	 * @Methods Name getContextProperty
	 * @Create In 2015年8月25日 By Jack
	 * @param name  属性的Key
	 * @return 值
	 */
	public static String getContextProperty(String name) {
		// TODO Auto-generated method stub
		return pc.getProperty(name);
	}

	/**
	 * 返回指定属性的值
	 * @Methods Name getContextProperty
	 * @Create In 2015年8月25日 By Jack
	 * @param name 属性的Key
	 * @param defaultValue 如果没找到的默认值
	 * @return 值，如没有返回defultValue
	 */
	public static String getContextProperty(String name, String defaultValue) {
		return pc.getProperty(name, defaultValue);
	}

    public static Boolean getBoolean(String name, Boolean defaultValue) {
        return pc.contains(name) ? Boolean.valueOf(getContextProperty(name)) : defaultValue;
    }

	/**
	 * 设置属性值
	 * @Methods Name setContextProperty
	 * @Create In 2015年8月25日 By Jack
	 * @param name 属性的Key
	 * @param value 属性的值
	 */
	public static void setContextProperty(String name, String value){
		pc.setProperty(name, value);
	}

	/**
	 * 格式化字符串
	 * @Methods Name fromatter
	 * @Create In 2015年8月25日 By Jack
	 * @param source 待替换字符串 ｛0｝起
	 * @param tagets 需要替换的内容
	 * @return String
	 */
	public static String fromatter(String source, Object[] tagets) {
		// TODO Auto-generated method stub
		return MessageFormat.format(source,tagets);
	}

}
