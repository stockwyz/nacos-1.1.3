package com.alibaba.nacos;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.ServletRequestWrapper;

import org.apache.catalina.connector.RequestFacade;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.Parameters;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;

import com.alibaba.nacos.config.server.constant.Constants;

/**
 * 
 * @author zht
 *
 */
@Aspect
@Configuration
public class NacosConfigAop {
	public NacosConfigAop() {
		System.out.println("");
	}
	
	@Around(value = "execution(* com.alibaba.nacos.config.server.controller.ConfigController.getConfig(..))")
    public Object serviceConfigInterceptor(ProceedingJoinPoint jp) {
		Object[] args = jp.getArgs();
		if(args != null && args[2] != null) {
			String dataId = String.valueOf(args[2]);
			if(dataId.endsWith(".properties") || dataId.endsWith(".yml") || dataId.endsWith(".yaml")) {
				int loc = dataId.indexOf(".properties");
				if(loc == -1) loc = dataId.indexOf(".yml");
				if(loc == -1) loc = dataId.indexOf(".yaml");
				args[2] = dataId.substring(0,  loc);
				System.out.println(args[2]);
			}
		}
		try {
			return jp.proceed(jp.getArgs());
		} catch (Throwable e) {  
	        throw new RuntimeException(e);  
	    }
	}
	
	@SuppressWarnings("unchecked")
	@Around(value = "execution(* com.alibaba.nacos.config.server.controller.ConfigController.listener(..))")
    public Object serviceConfigListenerInterceptor(ProceedingJoinPoint jp) {
		Object[] args = jp.getArgs();
		if(args != null && args[1] != null) {
			RequestFacade rf = (RequestFacade) ((ServletRequestWrapper) args[0]).getRequest();
			org.apache.catalina.connector.Request r = (org.apache.catalina.connector.Request) searchField(rf, RequestFacade.class, "request");
			org.apache.coyote.Request coyoteRequest = (org.apache.coyote.Request) searchField(r, org.apache.catalina.connector.Request.class, "coyoteRequest");
			Parameters param = coyoteRequest.getParameters();
			Map<String,ArrayList<String>> paramMap = (Map<String,ArrayList<String>>) searchField(param, Parameters.class, "paramHashValues");
			
			/*Parameters param = facade.getCoyoteRequest().getParameters();
			try {
				Field field = Parameters.class.getDeclaredField("paramHashValues");
				Map<String,ArrayList<String>> paramMap = (Map<String,ArrayList<String>>)field.get(param);
				paramMap.get("Listening-Configs");
			} catch (Exception e) {
				e.printStackTrace();
			} */
			
			String probeModify = rf.getParameter("Listening-Configs");
			try {
				probeModify = URLDecoder.decode(probeModify, Constants.ENCODE);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			if(StringUtils.isNotBlank(probeModify)) {
				if(probeModify.indexOf(".properties") != -1 || probeModify.indexOf(".yml") != -1 
						|| probeModify.indexOf(".yaml") != -1) {
					int loc = probeModify.indexOf(".properties");
					if(loc != -1) {
						probeModify = probeModify.substring(0, loc) + probeModify.substring(loc + 11);
					} else {
						loc = probeModify.indexOf(".yml");
						if(loc == -1) {
							probeModify = probeModify.substring(0, loc) + probeModify.substring(loc + 4);
						} else {
							loc = probeModify.indexOf(".yaml");
							if(loc == -1) {
								probeModify = probeModify.substring(0, loc) + probeModify.substring(loc + 5);
							}
						}
					}
				}
			}
		}
		
		try {
			return jp.proceed(jp.getArgs());
		} catch (Throwable e) {  
	        throw new RuntimeException(e);  
	    }
	}
	
	private Object searchField(Object m, Class<?> clazz, String fieldName) {
//		Parameters param = facade.getCoyoteRequest().getParameters(); (Map<String,ArrayList<String>>) paramHashValues
		Object obj = null;
		try {
			Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			obj =  field.get(m);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}
}
