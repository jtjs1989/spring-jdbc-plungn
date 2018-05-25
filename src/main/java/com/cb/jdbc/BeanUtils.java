package com.cb.jdbc;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;

public class BeanUtils {

	public static Object getProperty(Object bean, String name) {
		if (bean == null || StringUtils.isEmpty(name)) {
			return null;
		}
		Method method;
		try {
			method = bean.getClass().getMethod(
					"get" + name.substring(0, 1).toUpperCase()
							+ name.substring(1));
			return method.invoke(bean);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object getProperty(Object bean, String name, boolean type) {
		if (bean == null || StringUtils.isEmpty(name)) {
			return null;
		}
		Method method = null;
		try {
			if (type) {
				method = bean.getClass().getMethod("is" + name.substring(0, 1).toUpperCase() + name.substring(1));
			} else {
				method = bean.getClass().getMethod("get" + name.substring(0, 1).toUpperCase()+ name.substring(1));
			}
			return method.invoke(bean);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
