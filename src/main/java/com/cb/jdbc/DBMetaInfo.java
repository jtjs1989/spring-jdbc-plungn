package com.cb.jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import com.cb.jdbc.util.StringUtil;

/**
 * 缓存Bean 字段映射信息
 * @author chenbo
 *
 */
public class DBMetaInfo {

	/**
	 * 缓存类型与全量字段信息
	 */
	private static final Map<Class<?>, List<String>> colunmsMap = new HashMap<>();
	
	public static void initFields(Class<?> classType) {
		Field[] fields = classType.getDeclaredFields();
		List<String> fieldsList = new ArrayList<String>();
		for (Field field : fields) {
			if(Modifier.isStatic(field.getModifiers()) || 
					field.getAnnotation(Transient.class) != null) continue;
			Column column = field.getAnnotation(Column.class);
			if (column == null || StringUtils.isEmpty(column.name())) {
				fieldsList.add(field.getName());
			} else {
				fieldsList.add(column.name());
			}
		}
		colunmsMap.put(classType, fieldsList);
	}
	
	public static List<String> getFields(Class<?> classType) {
		return colunmsMap.get(classType);
	}
	
	public static String getFieldsStr(Class<?> classType) {
		if (colunmsMap.get(classType) != null) {
			return StringUtil.contract(colunmsMap.get(classType), ",");
		}
		return null;
	}
	
	public static boolean checkField(Class<?> classType, String filedName) {
		return getFields(classType).contains(filedName);
	}
}
