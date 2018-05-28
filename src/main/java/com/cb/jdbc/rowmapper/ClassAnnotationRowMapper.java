package com.cb.jdbc.rowmapper;

import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;

import com.cb.jdbc.annotation.AnnontationUtils;

public class ClassAnnotationRowMapper<T> extends BeanPropertyRowMapper<T> {

	private Map<String, PropertyDescriptor> mappedFields;

	private Set<String> mappedProperties;

	private Class<T> mappedClass;
	public ClassAnnotationRowMapper(){
	}
	public ClassAnnotationRowMapper(Class<T> mappedClass){
		super(mappedClass);
	}
	@Override
	protected void initialize(Class<T> mappedClass) {
		this.mappedClass = mappedClass;
		this.mappedFields = new HashMap<String, PropertyDescriptor>();
		this.mappedProperties = new HashSet<String>();
		PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(mappedClass);
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() != null) {
				Column column = AnnontationUtils.getFromField(Column.class, mappedClass, pd.getName());
				Transient tran = AnnontationUtils.getFromField(Transient.class, mappedClass, pd.getName());
				if (tran != null) {
					continue;
				}
				String columnName = column == null ? pd.getName() : column.name();
				this.mappedFields.put(columnName.toLowerCase(), pd);
				this.mappedProperties.add(pd.getName());
			}
		}
	}
	public Map<String, PropertyDescriptor> getMappedFields() {
		return mappedFields;
	}

	public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
		Assert.state(this.mappedClass != null, "Mapped class was not specified");
		T mappedObject = BeanUtils.instantiate(this.mappedClass);
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);
		initBeanWrapper(bw);

		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		Set<String> populatedProperties = (isCheckFullyPopulated() ? new HashSet<String>() : null);

		for (int index = 1; index <= columnCount; index++) {
			String column = JdbcUtils.lookupColumnName(rsmd, index);
			PropertyDescriptor pd = this.mappedFields.get(column.replaceAll(" ", "").toLowerCase());
			if (pd != null) {
				try {
					Object value = getColumnValue(rs, index, pd);
					/*if (logger.isDebugEnabled() && rowNumber == 0) {
						logger.debug("Mapping column '" + column + "' to property '" +
								pd.getName() + "' of type " + pd.getPropertyType());
					}*/
					try {
						bw.setPropertyValue(pd.getName(), value);
					}
					catch (TypeMismatchException e) {
							throw e;
					}
					if (populatedProperties != null) {
						populatedProperties.add(pd.getName());
					}
				}
				catch (NotWritablePropertyException ex) {
					throw new DataRetrievalFailureException(
							"Unable to map column " + column + " to property " + pd.getName(), ex);
				}
			}
		}
		return mappedObject;
	}
}
