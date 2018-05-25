package com.cb.jdbc.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author boll
 */
public class AnnontationUtils {

	public static <T extends Annotation> T getFromMethed(
			Class<T> annotationClazz, Object o, String methodName) {
		if (o == null) {
			return null;
		}
		Class<? extends Object> objectClass = o.getClass();
		try {
			Method method = objectClass.getDeclaredMethod(methodName);
			return method.getAnnotation(annotationClazz);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T extends Annotation> T getFromMethedOrType(
			Class<T> annotationClazz, Object obj, String methodName) {
		if (obj == null) {
			return null;
		}
		Class<? extends Object> objectClass = obj.getClass();
		T annotation = null;
		try {
			Method method = objectClass.getDeclaredMethod(methodName);
			annotation = method.getAnnotation(annotationClazz);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		if (annotation != null) {
			return annotation;
		}
		return objectClass.getAnnotation(annotationClazz);
	}

	public static <T extends Annotation> T getFromField(Class<T> annotationClazz, Class<?> obj, String filed) {
		try {
			Field field = obj.getDeclaredField(filed);
			return field.getAnnotation(annotationClazz);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return null;
		
	}
}
