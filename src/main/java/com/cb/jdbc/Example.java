package com.cb.jdbc;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.cb.jdbc.annotation.Operator.OperatorMark;
import com.cb.jdbc.util.StringUtil;

/**
 * Example example = new Example(a.class);
 * example.andEquals(column1, "aaa").andGranter(column2, 5).setPageNo(2).setPageSize(10);
 * 
 * List<T> list = dao.selectByExample(example);
 * @author chenbo
 *
 */
public class Example extends QueryVo{

	private List<String> selectProperties = new ArrayList<>();
	
	private Class<?> classType;
	
	private List<String> keys = new ArrayList<>();
	
	private List<Object> values = new ArrayList<>();
	
	private String groupBy;
	
	public Example(Class<?> classType) {
		notNull(DBMetaInfo.getFields(classType), "class :"+ classType.getName() + " not contained by DbMetaInfo");
		this.classType = classType;
	}
	
	public Class<?> getClassType() {
		return classType;
	}
	/**
	 * field = value
	 * @param field
	 * @param value
	 * @return
	 */
	public Example andEquals(String field, Object value) {
		notEmpty(field, "field");
		keys.add(field + OperatorMark.EQUAL.getOpetator() + "? ");
		values.add(value);
		return this;
	}
	/**
	 * field <> value
	 * @param field
	 * @param value
	 * @return
	 */
	public Example andNotEquals(String field, Object value) {
		notEmpty(field, "field");
		keys.add(field + OperatorMark.NOTEQUAL.getOpetator() + "? ");
		values.add(value);
		return this;
	}
	/**
	 * field in (value...)
	 * @param field
	 * @param value
	 * @return
	 */
	public Example andIn(String field, List<?> value) {
		notEmpty(field, "field");
		int i = 0;
		StringBuilder placeholder = new StringBuilder("(");
		while(i < value.size()) {
			placeholder.append("?");
			if (i < value.size()-1) {
				placeholder.append(", ");
			}
			i ++;
		}
		placeholder.append(")");
		keys.add(field + OperatorMark.IN.getOpetator() + placeholder.toString());
		values.addAll(value);
		return this;
	}
	/**
	 * field not in (value...)
	 * @param field
	 * @param value
	 * @return
	 */
	public Example andNotIn(String field, List<Object> value) {
		notEmpty(field, "field");
		int i = 0;
		StringBuilder placeholder = new StringBuilder("(");
		while(i < value.size()) {
			i ++;
			placeholder.append("?");
			if (i < value.size()-1) {
				placeholder.append(", ");
			}
		}
		placeholder.append(")");
		keys.add(field + OperatorMark.NOT_IN.getOpetator() + placeholder.toString());
		values.addAll(value);
		return this;
	}
	/**
	 * field > value
	 * @param field
	 * @param value
	 * @return
	 */
	public Example andGranter(String field, Object value) {
		notEmpty(field, "field");
		keys.add(field + OperatorMark.GREATER.getOpetator() + "? ");
		values.add(value);
		return this;
	}
	
	public Example andGranterOrEquals(String field, Object value) {
		notEmpty(field, "field");
		keys.add(field + OperatorMark.GREATEROREQUAL.getOpetator() + "? ");
		values.add(value);
		return this;
	}
	
	public Example andLess(String field, Object value) {
		notEmpty(field, "field");
		keys.add(field + OperatorMark.LESS.getOpetator() + "? ");
		values.add(value);
		return this;
	}
	
	public Example andLessOrEquals(String field, Object value) {
		notEmpty(field, "field");
		keys.add(field + OperatorMark.LESSOREQUAL.getOpetator() + "? ");
		values.add(value);
		return this;
	}
	
	public Example orderBy(String orderBy) {
		super.setOrderBy(orderBy);
		return this;
	}
	
	public Example groupBy(String groupBy) {
		this.groupBy = groupBy;
		return this;
	}
	public Example selectProperties(String...strings) {
		for (String string : strings) {
			selectProperties.add(string);
		}
		return this;
	}
	
	public String getSelectProperties() {
		if (selectProperties == null || selectProperties.isEmpty()) {
			return null;
		} else {
			return StringUtil.contract(selectProperties,",");
		}
	}
	
	public String getOrderByString() {
		return StringUtils.isEmpty(getOrderBy()) ? "" : " order by " + getOrderBy();
	}
	
	public String getWhere() {
		if (keys.isEmpty()) {
			return "";
		} else {
			return " where " + StringUtil.contract(keys, "and ");
		}
	}
	
	public List<Object> getValues() {
		return this.values;
	}
	
	public String getPageString() {
		if (getPageSize() > 0) {
			return " limit " + (getPageNo() - 1) * getPageSize() + "," + getPageSize();
		} else {
			return "";
		}
	}
	
	public String getGroupBy() {
		return StringUtils.isEmpty(groupBy) ? "" : " group by " +groupBy;
	}
	
	private void notEmpty(String object, String name) {
		if (StringUtils.isEmpty(object)) {
            throw new IllegalStateException(name + " is empty");
        }
	}
	private void notNull(final Object object, final String name) {
        if (object == null) {
            throw new IllegalStateException(name + " is null");
        }
    }
}
