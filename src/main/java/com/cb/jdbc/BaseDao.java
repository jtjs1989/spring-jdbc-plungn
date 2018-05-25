package com.cb.jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.cb.jdbc.annotation.AnnontationUtils;
import com.cb.jdbc.annotation.Operator;
import com.cb.jdbc.annotation.OptimisticLock;
import com.cb.jdbc.annotation.Order;
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
	
	public BaseDao(Class<T> classType) {
		this.type = classType;
	}
	
	public String getNomalSqlHead(String querySql) {
		return new StringBuilder().append(selectHead).append(where).append(" ").append(querySql).toString();
	}

	protected JdbcTemplate jdbcTemplate;
	
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}
	@Resource(name="jdbcTemplate")
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public <T> T queryObject(String sql, RowMapper<T> rowMapper, Object... objects ) {
		return queryObject(getJdbcTemplate(), sql, rowMapper, objects);
	}
	
	public <T> T queryObject(JdbcTemplate jdbcTemplate,String sql, RowMapper<T> rowMapper, Object... objects ) {
		List<T> resultList = jdbcTemplate.query(sql, objects, rowMapper);
		return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
	}
	
	public <T> T getObjById(String tableName,RowMapper<T> rowMapper, long id) {
		return getObjById(tableName, "F_id", rowMapper, id);
	}
	
	public <T> T getObjById(String tableName, String idField, RowMapper<T> rowMapper, Object id) {
		String sql = selectHead + tableName + where + idField + " = ?";
		return queryObject(sql, rowMapper, id);
	}
	
	public <T> T getObjById(Class<?> c,RowMapper<T> rowMapper, long id) {
		TableName tableName = c.getAnnotation(TableName.class);
		return getObjById(tableName.value(), rowMapper, id);
	}
	
	public <T> T getObjById(Class<?> c, String idField, RowMapper<T> rowMapper, long id) {
		TableName tableName = c.getAnnotation(TableName.class);
		return getObjById(tableName.value(), idField, rowMapper, id);
	}
	
	public <T> void  deleteObjById(Class<?> c, long id){
		deleteObjById(c, "F_id", id);
	}
	
	public <T> int  deleteObjById(Class<?> c, String keyField, long id){
		TableName tableName = c.getAnnotation(TableName.class);
		StringBuilder sql = new StringBuilder("delete from ").append(tableName.value())
				.append(where).append(keyField).append(" = ?");
		log.info(stringFormat(sql.toString(), "\\?", id));
		return getJdbcTemplate().update(sql.toString(), id);
	}
	
	
	@Override
	public int insert(T record) {
		return addObj(record);
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public T selectByKey(Object key) {
		// TODO Auto-generated method stub
		return null;
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

	protected int addObj(T obj) {
		TableName tableName = obj.getClass().getAnnotation(TableName.class);
		return addObj(getJdbcTemplate(), tableName.value(), obj);
	}
	protected int addObj(JdbcTemplate jdbcTemplate,String tableName, T obj) {
		if (obj == null) {
			throw new NullPointerException("addObj 方法参数不能为空");
		}
		UpdateSqlHelper sqlHelper = getAddSql(obj);
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
	 * 按主键更新
	 * @param tableName
	 * @param keyField
	 * @param updateMap
	 * @param obj
	 */
	protected <T> int updateObj(String tableName,String keyField, Map<String, Object> updateMap,T obj) {
		StringBuilder sbField=new StringBuilder();
		List<Object> args=new ArrayList<Object>();
		sbField.append("update ").append(tableName).append(" set ");
		for (String field : updateMap.keySet()) {
			if (field.equals(keyField)) {
				continue;
			}
			Column column = AnnontationUtils.getFromField(Column.class, obj.getClass(), field);
			sbField.append(column.value()).append(" = ? ,");
			args.add(updateMap.get(field));
		}
		args.add(updateMap.get(keyField));
		Column column = AnnontationUtils.getFromField(Column.class, obj.getClass(), keyField);
		String sql = sbField.substring(0, sbField.length()-1) + " where " + column.value() +" = ?";
		log.info(stringFormat(sql, "\\?", args.toArray()));
		return this.getJdbcTemplate().update(sql, args.toArray());
	}
	
	protected <T> void updateObj(String keyField, Map<String, Object> updateMap,T obj) {
		TableName tableName = obj.getClass().getAnnotation(TableName.class);
		updateObj(tableName.value(), keyField, updateMap, obj);
	}
	/**
	 * 更新对象的所有字段  主键作为更新条件，不更新creatTime和updateTime字段
	 * @param keyField  数据库主键字段
	 * @param obj
	 */
	protected <T> int updateObj(String keyField, T obj){
		return updateObj(keyField, obj, false);
	}
	protected <T> int updateObj(String keyField, T obj, boolean isOptimistic){
		StringBuilder sbField=new StringBuilder();
		List<Object> args=new ArrayList<Object>();
		TableName tableName = obj.getClass().getAnnotation(TableName.class);
		sbField.append("update ").append(tableName.value()).append(" set ");
		Field [] fields=obj.getClass().getDeclaredFields();
		int version = -1;
		String optimisticField = null;
		if (fields != null && fields.length > 0) {
			Object value = null;
			Object keyValue = null; //主键值
			for (Field field : fields) {
				String name = field.getName();
				if(Modifier.isStatic(field.getModifiers())
						|| name.toLowerCase().equals("createtime")
						|| name.toLowerCase().equals("updatetime")){
					continue;
				} 
				value=BeanUtils.getProperty(obj, name);
				if(value==null) continue;
				Column column = AnnontationUtils.getFromField(Column.class, obj.getClass(), name);
				if (column == null) continue;
				if (isOptimistic && version == -1) {
					OptimisticLock lock = AnnontationUtils.getFromField(OptimisticLock.class, obj.getClass(), name);
					if (lock != null) {
						version = (Integer) value;
						value = version + 1;
						optimisticField = column.value();
					}
				}
				if(!keyField.equalsIgnoreCase(column.value())){
					sbField.append(column.value()).append(" = ?,");
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
				.append(keyField).append(" = ?");
			
			log.info(stringFormat(sbField.toString(), "\\?", args.toArray()));
			return this.getJdbcTemplate().update(sbField.toString(), args.toArray());
		}
		return 0;
	}
	public <T> List<T> queryObj(String tableName, Map<String, Object> queryMap, Class<?> obj, RowMapper<T> rowMapper){
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
			sbField.append(column.value()).append(" = ? and ");
			values.add(queryMap.get(field));
			empty = false;
		}
		if (!empty) {
			sbField.delete(sbField.length() - 4, sbField.length()-1);
		}
		if (log.isDebugEnabled()) {
			log.info("sql=" + sbField.toString());
		}
		return getJdbcTemplate().query(sbField.toString(), rowMapper, values.toArray());
	}
	public <T> List<T> queryObj(Map<String, Object> queryMap, Class<?> obj, RowMapper<T> rowMapper) {
		TableName tableName = obj.getAnnotation(TableName.class);
		return queryObj(tableName.value(), queryMap, obj, rowMapper);
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
		return queryList(getJdbcTemplate(), query, pageIndex, pageSize, rowMapper, db_name, orderField, null);
	}
	public <T> List<T> queryList(QueryVo query, int pageIndex, int pageSize, RowMapper<T> rowMapper, String db_name, String orderField, Sequence seq){
		return queryList(getJdbcTemplate(), query, pageIndex, pageSize, rowMapper, db_name, orderField, seq);
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
		return countQueryList(getJdbcTemplate(), query, db_name);
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
			Object value=null;
			//先对字段进行排序，order 从小到大
			Arrays.sort(fields, new Comparator<Field>() {
				@Override
				public int compare(Field arg1, Field arg2) {
					Order o1 = arg1.getAnnotation(Order.class);
					Order o2 = arg2.getAnnotation(Order.class);
					int fieldOrder1 = (o1 == null ? 100 : o1.value());
					int fieldOrder2 = (o2 == null ? 100 : o2.value());
					return fieldOrder1 - fieldOrder2;
				}
			} );
			for(int i=0;i<fields.length;i++){
				name=fields[i].getName();
				Class<?> type = fields[i].getType();
				if(Modifier.isStatic(fields[i].getModifiers())) continue;
				value=BeanUtils.getProperty(queryVo, name);
				if(value==null) continue;
				if (type == String.class) {
					String str = (String)value;
					if (StringUtils.isEmpty(str)) {
						continue;
					}
				}
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
//						throw new YeahkaException("mysql in or notin operator query must use list parameter");
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
					sbField.append(column.value()).append(operator).append(str.toString() + " and ");
				}else {
					sbField.append(column.value()).append(operator).append(" ? and ");
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
		return query(getJdbcTemplate(), sql, rowMapper, args);
	}
	protected <T> List<T> query(JdbcTemplate jdbcTemplate,String sql, RowMapper<T> rowMapper){
		log.info(sql);
		return jdbcTemplate.query(sql, rowMapper);
	}
	protected <T> List<T> query(String sql, RowMapper<T> rowMapper){
		return query(getJdbcTemplate(), sql, rowMapper);
	}
	protected static String getTable(String tableName, long sharding, int N) {
		int i = 1;
		while(Math.pow(10, i) < N) {
			i++;
		}
		int suffex = (int) (sharding % N);
		String suffexStr = String.valueOf(suffex);
		int dis = i - suffexStr.length();
		if (dis == 3) {
			suffexStr = "000"+suffexStr;
		}else if (dis == 2) {
			suffexStr = "00" + suffexStr;
		}else if (dis == 1) {
			suffexStr = "0" + suffexStr;
		}
		return tableName + "_" + suffexStr;
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
	public static void main(String[] args) {
//		Pattern patten = Pattern.compile("(.*)and(\\s)*$");
//		Matcher matcher = patten.matcher("f_uid = ? and ");
//		System.out.println(matcher.matches());
	}
}
