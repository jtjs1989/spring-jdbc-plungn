package com.cb.jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.cb.jdbc.annotation.AnnontationUtils;
import com.cb.jdbc.annotation.Operator;
import com.cb.jdbc.annotation.TableName;
import com.cb.jdbc.mapper.BaseMapper;

public abstract class BaseDao<T> implements BaseMapper<T>{

	private final static Logger log = LoggerFactory.getLogger(BaseDao.class);
	protected static final String selectHead = "select * from ";
	protected static final String where = " where ";
	protected static final String orderBy = " order by ";
	protected static final String order_desc = " desc ";
	protected static final String order_asc = " asc ";
	protected static final String limit = " limit ";
	protected static final String update = "update ";
	protected static final String and = " and ";
	private static final Pattern pattern = Pattern.compile("(.*)and(\\s)*$");
	
	private Class<T> type;
	
	private RowMapper<T> rowMapper;
	/**
	 * the key id name
	 */
	private String keyIdName;
	
	private String tableName;
	
	public BaseDao(Class<T> classType) {
		this.type = classType;
		findKeyId(classType);
		initTableName(classType);
	}
	
	private void findKeyId(Class<T> classType) {
		Field[] fields = classType.getFields();
		for (Field field : fields) {
			if (field.getAnnotation(Id.class) != null) {
				Column column = field.getAnnotation(Column.class);
				keyIdName = (column == null ? field.getName() : column.name());
				break;
			}
		}
	}
	private void initTableName(Class<T> classType) {
		this.tableName = classType.getAnnotation(TableName.class).value();
	}
	public String getNomalSqlHead(String querySql) {
		return new StringBuilder().append(selectHead).append(where).append(" ").append(querySql).toString();
	}

	protected JdbcTemplate jdbcTemplate;
	
	@Resource(name="jdbcTemplate")
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	
	public <T> T queryObject(JdbcTemplate jdbcTemplate,String sql, RowMapper<T> rowMapper, Object... objects ) {
		List<T> resultList = jdbcTemplate.query(sql, objects, rowMapper);
		return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
	}
	
	public T getObjById(Object id) {
		String sql = selectHead + tableName + where + keyIdName + " = ?";
		return jdbcTemplate.queryForObject(sql, rowMapper, id);
	}
	
	@Override
	public int insert(T record) {
		if (record == null) {
			throw new NullPointerException("必传参数为空");
		}
		UpdateSqlHelper sqlHelper = getAddSql(record);
		StringBuilder sql=new StringBuilder();
		sql.append("insert into ").append(tableName).append(" ");
		sql.append(sqlHelper.getFieldSql()).append(" values ").append(sqlHelper.getMarkSql());
		final String sqlStr = sql.toString();
		final List<Object> argsArr = sqlHelper.getArgs();
		if (log.isDebugEnabled()) {
			log.info(stringFormat(sqlStr, "\\?", argsArr.toArray()));
		}
		return jdbcTemplate.update(sqlStr, argsArr.toArray());
	}

	@Override
	public int insertNotNull(T record) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int update(T record) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int updateNotNull(T record) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int deleteById(Object key) {
		StringBuilder sql = new StringBuilder("delete from ").append(tableName)
				.append(where).append(keyIdName).append(" = ?");
		return jdbcTemplate.update(sql.toString(), key);
	}

	@Override
	public T selectByKey(Object key) {
		String sql = selectHead + tableName + where + keyIdName + " = ?";
		return jdbcTemplate.queryForObject(sql, rowMapper, key);
	}

	@Override
	public List<T> select(T record) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int selectCount(T record) {
		// TODO Auto-generated method stub
		return 0;
	}

	private <T> UpdateSqlHelper getAddSql(T obj) {
		StringBuilder sbField=new StringBuilder();
		StringBuilder sbArgs=new StringBuilder();
		List<Object> args=new ArrayList<Object>();
		sbField.append(" ( ");
		sbArgs.append(" ( ");
		Field [] fields=obj.getClass().getDeclaredFields();
		if(fields!=null&&fields.length>0){
			String name="";
			Object value=null;
			for(int i=0;i<fields.length;i++){
				name=fields[i].getName();
				if(Modifier.isStatic(fields[i].getModifiers())) continue;
				value=BeanUtils.getProperty(obj, name);
				String columnName;
				Column column = AnnontationUtils.getFromField(Column.class, obj.getClass(), name);
				if (column == null) {
					Transient tr = AnnontationUtils.getFromField(Transient.class, obj.getClass(), name);
					if (tr != null) {  //不映射数据库字段
						continue;
					} else {
						columnName = name;
					}
				} else {
					columnName = column.name();
				}
				sbField.append(columnName);
				if(i<fields.length-1) sbField.append(", ");
				sbArgs.append("? ");
				args.add(value);
				if(i<fields.length-1) sbArgs.append(", ");
			}
		}
		if(sbField.toString().trim().endsWith(",")){
			sbField=new StringBuilder(sbField.substring(0, sbField.lastIndexOf(",")));
		}
		if(sbArgs.toString().trim().endsWith(",")){
			sbArgs=new StringBuilder(sbArgs.substring(0, sbArgs.lastIndexOf(",")));
		}
		sbField.append(" ) ");
		sbArgs.append(" ) ");
		UpdateSqlHelper sqlHelper = new UpdateSqlHelper();
		sqlHelper.setArgs(args);
		sqlHelper.setFieldSql(sbField);
		sqlHelper.setMarkSql(sbArgs);
		return sqlHelper;
	}
	
	protected int update(String sql, Object...args){
		return update(jdbcTemplate, sql, args);
	}
	
	protected int update(JdbcTemplate jdbcTemplate, String sql, Object...args){
		log.info(stringFormat(sql, "\\?", args));
		return jdbcTemplate.update(sql, args);
	}
	
	/**
	 * 更新对象的所有字段  主键作为更新条件，不更新creatTime和updateTime字段
	 * @param keyField  数据库主键字段
	 * @param obj
	 */
	protected int updateObj(T obj){
		StringBuilder sbField=new StringBuilder();
		List<Object> args=new ArrayList<Object>();
		sbField.append("update ").append(tableName).append(" set ");
		Field [] fields=obj.getClass().getDeclaredFields();
		if (fields != null && fields.length > 0) {
			Object value = null;
			Object keyValue = null; //主键值
			for (Field field : fields) {
				String name = field.getName();
				if(Modifier.isStatic(field.getModifiers())){
					continue;
				} 
				value=BeanUtils.getProperty(obj, name);
				if(value==null) continue;
				Column column = AnnontationUtils.getFromField(Column.class, obj.getClass(), name);
				if (column == null) continue;
				if(!keyIdName.equalsIgnoreCase(column.name())){
					sbField.append(column.name()).append(" = ?,");
					args.add(value);
				} else {
					keyValue = value;
				}
			}
			if (keyValue != null) {
				args.add(keyValue);
			} else {
				throw new RuntimeException("更新没设置主键条件");
			}
			sbField.deleteCharAt(sbField.length()-1).append(" where ")
				.append(keyIdName).append(" = ?");
			if (log.isDebugEnabled()) {
				log.info(stringFormat(sbField.toString(), "\\?", args.toArray()));
			}
			return jdbcTemplate.update(sbField.toString(), args.toArray());
		}
		return 0;
	}
	public List<T> queryObj(String tableName, Map<String, Object> queryMap, Class<?> obj, RowMapper<T> rowMapper){
		StringBuilder sbField = new StringBuilder("select * from ");
		sbField.append(tableName).append(" where ");
		List<Object> values = new ArrayList<Object>();
		boolean empty = true;
		for (String field : queryMap.keySet()) {
			Object value = queryMap.get(field);
			if (value == null) {
				continue;
			}
			Column column = AnnontationUtils.getFromField(Column.class, obj, field);
			sbField.append(column.name()).append(" = ? and ");
			values.add(queryMap.get(field));
			empty = false;
		}
		if (!empty) {
			sbField.delete(sbField.length() - 4, sbField.length()-1);
		}
		if (log.isDebugEnabled()) {
			log.info("sql=" + sbField.toString());
		}
		return jdbcTemplate.query(sbField.toString(), rowMapper, values.toArray());
	}
	/**
	 * 得到完整的sql 语句
	 * @param sqlHelper
	 * @param tableName
	 * @param orderField
	 * @param pageIndex
	 * @param pageSize
	 * @return
	 */
	private QuerySqlHelper getFullSql(QuerySqlHelper sqlHelper, String tableName,String orderField, Sequence seq, int pageIndex, int pageSize){
		StringBuilder querySql = sqlHelper.getSql();
		Matcher matcher = pattern.matcher(querySql);
		if (matcher.matches()) {
			querySql = new StringBuilder(matcher.group(1));
		}
		if (querySql.length() > 0) {
			querySql.insert(0, where);
		}
		querySql.insert(0, selectHead + tableName);
		if (StringUtils.isNotEmpty(orderField)) {
			querySql.append(" order by ").append(orderField);
			if (seq != null && seq.equals(Sequence.asc)) {
				querySql.append(order_asc);
			}else {
				querySql.append(order_desc);
			}
		}
		if (pageSize > 0) {
			querySql.append(" limit ?,?");
			sqlHelper.getArgs().add(pageIndex);
			sqlHelper.getArgs().add(pageSize);
		}
		if (log.isDebugEnabled()) {
			log.debug(stringFormat(querySql.toString(), "\\?", sqlHelper.getArgs().toArray()));
		}
		sqlHelper.setSql(querySql);
		return sqlHelper;
	}
	
	public String stringFormat(String string, String regex, Object...args){
		try {
			if (args == null || args.length == 0) {
				return string;
			}
			for (Object object : args) {
				string = string.replaceFirst(regex, object.toString());
			}
			return string;
		} catch (Exception e) {
			return string;
		}
	}
	public <T> List<T> queryList(JdbcTemplate jdbcTemplate,QueryVo query, int pageIndex, int pageSize, RowMapper<T> rowMapper, String db_name, String orderField, Sequence seq){
		QuerySqlHelper sqlHelper = getQuerySql(query);
		sqlHelper = getFullSql(sqlHelper, db_name, orderField, seq, pageIndex, pageSize);
		StringBuilder querySql = sqlHelper.getSql();
		List<Object> args = sqlHelper.getArgs();
		return jdbcTemplate.query(querySql.toString(), rowMapper, args.toArray());
	}
	public List<Map<String, Object>> queryList(JdbcTemplate jdbcTemplate,QueryVo query, int pageIndex, int pageSize, String db_name, String orderField){
		QuerySqlHelper sqlHelper = getQuerySql(query);
		sqlHelper = getFullSql(sqlHelper, db_name, orderField,null, pageIndex, pageSize);
		StringBuilder querySql = sqlHelper.getSql();
		List<Object> args = sqlHelper.getArgs();
		return jdbcTemplate.queryForList(querySql.toString(), args.toArray());
	}
	public <T> List<T> queryList(QueryVo query, int pageIndex, int pageSize, RowMapper<T> rowMapper, String db_name, String orderField){
		return queryList(jdbcTemplate, query, pageIndex, pageSize, rowMapper, db_name, orderField, null);
	}
	public <T> List<T> queryList(QueryVo query, int pageIndex, int pageSize, RowMapper<T> rowMapper, String db_name, String orderField, Sequence seq){
		return queryList(jdbcTemplate, query, pageIndex, pageSize, rowMapper, db_name, orderField, seq);
	}
	public <T> List<T> queryList(QueryVo query, RowMapper<T> rowMapper, String tableName, String orderField){
		return queryList(jdbcTemplate, query, 0, 0,rowMapper, tableName, orderField, null);
	}
	public <T> List<T> queryList(QueryVo query, RowMapper<T> rowMapper, String tableName,int pageIndex, int pageSize){
		return queryList(query, pageIndex, pageSize, rowMapper, tableName, null, null);
	}
	public long countQueryList(JdbcTemplate template, QueryVo query, String db_name){
		QuerySqlHelper sqlHelper = getQuerySql(query);
		StringBuilder querySql = sqlHelper.getSql();
		List<Object> args = sqlHelper.getArgs();
		Matcher matcher = pattern.matcher(querySql);
		if (matcher.matches()) {
			querySql = new StringBuilder(matcher.group(1));
		}
		if (querySql.length() > 0) {
			querySql.insert(0, where);
		}
		querySql.insert(0, "select count(0) from " + db_name);
		log.info(stringFormat(querySql.toString(), "\\?", args.toArray()));
		return template.queryForObject(querySql.toString(), args.toArray(), Long.class);
	}
	public long countQueryList(QueryVo query, String db_name){
		return countQueryList(jdbcTemplate, query, db_name);
	}
//	public long countQueryList(QueryVo query){
//		return countQueryList(getJdbcTemplate(), query, getDBName());
//	}
	/**
	 * 通过queryVo 组装查询的sql 语句
	 * @param queryVo
	 * @return
	 */
	public QuerySqlHelper getQuerySql(QueryVo queryVo) {
		QuerySqlHelper sqlHelper = new QuerySqlHelper();
		StringBuilder sbField=new StringBuilder();
		List<Object> args=new ArrayList<Object>();
		Field [] fields=queryVo.getClass().getDeclaredFields();
		if(fields!=null&&fields.length>0){
			String name="";
			for(int i=0;i<fields.length;i++){
				Object value=null;
				name=fields[i].getName();
				Class<?> type = fields[i].getType();
				if(Modifier.isStatic(fields[i].getModifiers())) continue;
				value=BeanUtils.getProperty(queryVo, name);
				if(value == null) continue;
				
				Column column = AnnontationUtils.getFromField(Column.class, queryVo.getClass(), name);
				if (column == null) continue;
				
				boolean isInOperator = false;
				int listSize = 0;
				Operator mark = AnnontationUtils.getFromField(Operator.class, queryVo.getClass(), name);
				if (mark != null && (Operator.OperatorMark.IN.equals(mark.value())
						|| Operator.OperatorMark.NOT_IN.equals(mark.value()))) {
					if (type == List.class) {
						List<?> list = (List<?>)value;
						if (list.isEmpty()) {
							continue;
						}
						isInOperator = true;
						listSize = list.size();
						args.addAll(list);
					}else {
						throw new RuntimeException("mysql in or notin operator query must use list parameter");
					}
				}
				String operator = (mark == null ? " = " : mark.value().getOpetator());
				if (isInOperator) {
					StringBuilder str = new StringBuilder("(");
					for (int j = 0; j < listSize; j++) {
						str.append("?");
						if (j < listSize - 1) {
							str.append(", ");
						}
					}
					str.append(")");
					sbField.append(column.name()).append(operator).append(str.toString() + " and ");
				}else {
					sbField.append(column.name()).append(operator).append(" ? and ");
					args.add(value);
				}
			}
		}
		sqlHelper.setArgs(args);
		sqlHelper.setSql(sbField);
		return sqlHelper;
	}
	protected <T> List<T> query(JdbcTemplate jdbcTemplate,String sql, RowMapper<T> rowMapper, Object...args){
		log.info(stringFormat(sql, "\\?", args));
		return jdbcTemplate.query(sql, rowMapper, args);
	}
	protected <T> List<T> query(String sql, RowMapper<T> rowMapper, Object...args){
		return query(jdbcTemplate, sql, rowMapper, args);
	}
	protected <T> List<T> query(JdbcTemplate jdbcTemplate,String sql, RowMapper<T> rowMapper){
		log.info(sql);
		return jdbcTemplate.query(sql, rowMapper);
	}
	protected <T> List<T> query(String sql, RowMapper<T> rowMapper){
		return query(jdbcTemplate, sql, rowMapper);
	}
	
	protected String getInSql(int size) {
		if (size == 0) {
			return "";
		}
		String sql = "(";
		while(size-- > 0) {
			sql += "?,";
		}
		sql = sql.substring(0, sql.length()-1);
		sql += ")";
		return sql;
	}

}
