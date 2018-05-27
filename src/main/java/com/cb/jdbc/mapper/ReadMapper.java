package com.cb.jdbc.mapper;

import java.util.List;

import com.cb.jdbc.QueryVo;

public interface ReadMapper<T> {

	T selectByKey(Object key);
	
	List<T> select(T record);
	
	List<T> queryList(QueryVo query);
	
	int selectCount(T record);
	
}
