package com.cb.jdbc;

import java.util.ArrayList;
import java.util.List;

public class UpdateSqlHelper {

	private StringBuilder fieldSql = new StringBuilder();
	
	private StringBuilder markSql = new StringBuilder();
	
	private List<Object> args = new ArrayList<Object>();

	public StringBuilder getFieldSql() {
		return fieldSql;
	}

	public void setFieldSql(StringBuilder fieldSql) {
		this.fieldSql = fieldSql;
	}

	public StringBuilder getMarkSql() {
		return markSql;
	}

	public void setMarkSql(StringBuilder markSql) {
		this.markSql = markSql;
	}

	public List<Object> getArgs() {
		return args;
	}

	public void setArgs(List<Object> args) {
		this.args = args;
	}
	
}
