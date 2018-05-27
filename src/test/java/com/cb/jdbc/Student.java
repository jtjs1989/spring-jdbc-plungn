package com.cb.jdbc;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Id;

public class Student implements Serializable{
	
	@Id
	private Long id;
	@Column(name="name")
	private String name;
	@Column(name="height")
	private Double height;
	@Column(name="age")
	private Integer age;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Double getHeight() {
		return height;
	}
	public void setHeight(Double height) {
		this.height = height;
	}
	public Integer getAge() {
		return age;
	}
	public void setAge(Integer age) {
		this.age = age;
	}
	
}
