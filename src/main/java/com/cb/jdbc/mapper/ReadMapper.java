package com.cb.jdbc.mapper;

import java.util.List;

public interface ReadMapper<T> {

	T selectByKey(Object key);
	
	List<T> select(T record);
	
	int selectCount(T record);
	
}
