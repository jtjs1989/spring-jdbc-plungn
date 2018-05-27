package com.cb.jdbc;
/**
 * 查询类基类  提供分页查询和排序
 * pageSize > 0 && pageNo > 0 开启分页查询
 * @author chenbo
 *
 */
public abstract class QueryVo {

	private int pageSize;
	
	private int pageNo;
	
	private String orderBy;

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}
	
}
