package com.zhuoan.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;

/**
 * 
 * @author 
 * 
 */
public class PropertiesUtil {
	//private static final Log logger = LogFactory.getLog(PropertiesUtil.class);
	
	private Properties properties = new Properties();

	public PropertiesUtil(String propertieName) {
		try {
			Properties properties = new Properties();
			InputStream is = this.getClass().getResourceAsStream("/" + propertieName);
			properties.load(is);
			Map<String, String> params = new HashMap<String, String>();
			for (Object keyObj : properties.keySet()) {
				String key = keyObj.toString();
				String value = properties.getProperty(key);
				params.put("<" + key + ">", value);
			}
			for (Object keyObj : properties.keySet()) {
				String key = keyObj.toString();
				String value = properties.getProperty(key);
				for (String paramKey : params.keySet()) {
					if(value.contains(paramKey)){
						value = value.replaceAll(paramKey, params.get(paramKey));
					}
				}
				this.properties.put(keyObj, value);
			}
			is.close();
			//logger.info("load properties:" + JSON.toJSONString(this.properties));
		} catch (IOException e) {
			//logger.error(e);
		}
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	public Integer getInteger(String key) {
		return Integer.valueOf(get(key));
	}

	public Properties getProperties() {
		return properties;
	}
}