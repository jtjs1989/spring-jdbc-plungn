package com.cb.jdbc.util;

import java.util.Collection;
/**
 * 提供String操作工具类
 * @author chenbo
 *
 */
public class StringUtil {

	/**
	 * 字符串拼接
	 * @param list 源集合
	 * @param sperator 拼接分隔符
	 * @return
	 */
	public static String contract(Collection<String> list, String sperator) {
		StringBuilder sb = new StringBuilder();
		for (String string : list) {
			sb.append(string).append(sperator);
		}
		sb.delete(sb.length()-sperator.length(), sb.length());
		return sb.toString();
	}
}
