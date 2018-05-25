package com.cb.jdbc;

import java.util.ArrayList;
import java.util.List;

public class QuerySqlHelper {

	private StringBuilder sql = new StringBuilder();
	private List<Object> args = new ArrayList<Object>();
	public StringBuilder getSql() {
		return sql;
	}
	public void setSql(StringBuilder sql) {
		this.sql = sql;
	}
	public List<Object> getArgs() {
		return args;
	}
	public void setArgs(List<Object> args) {
		this.args = args;
	}
	
}
