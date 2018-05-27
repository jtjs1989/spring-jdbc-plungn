package com.cb.jdbc;

import org.springframework.stereotype.Service;

@Service
public class StudentDaoImpl extends BaseDao<Student> implements StudentDao {

	public StudentDaoImpl(Class<Student> classType) {
		super(classType);
	}

}
