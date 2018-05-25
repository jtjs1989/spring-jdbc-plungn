package com.cb.jdbc.mapper;

public interface WriteMapper<T> {

	int insert(T record);
	
	int insertNotNull(T record);
	
	int update(T record);
	
	int updateNotNull(T record);
	
	int deleteById(Object key);
	
}
