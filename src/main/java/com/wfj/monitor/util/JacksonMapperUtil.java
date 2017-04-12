/**
 * @Probject Name: netty-wfj-base-dev
 * @Path: com.wfj.netty.utilJacksonMapperUtil.java
 * @Create By Jack
 * @Create In 2015年8月27日 下午7:57:58
 * TODO
 */
package com.wfj.monitor.util;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import java.io.IOException;

/**
 * @Class Name JacksonMapperUtil
 * @Author Jack
 * @Create In 2015年8月27日
 */
public class JacksonMapperUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String objectToJson(Object source) throws JsonGenerationException, JsonMappingException, IOException {
        //解析器支持解析单引号
        mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
        //解析器支持解析结束符
        mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.setVisibility(JsonMethod.FIELD, Visibility.ANY);
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, false);

        return mapper.writeValueAsString(source);
    }

    public static <T> Object jsonToObject(String source, Class<T> destin)
            throws JsonParseException, JsonMappingException,
            IOException {
        return mapper.readValue(source, destin);
    }
}
