package com.cb.jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.cb.jdbc.annotation.AnnontationUtils;
import com.cb.jdbc.annotation.Operator;
import com.cb.jdbc.mapper.BaseMapper;
import com.cb.jdbc.rowmapper.ClassAnnotationRowMapper;

public abstract class BaseDao<T> implements BaseMapper<T>{

	private final static Logger log = LoggerFactory.getLogger(BaseDao.class);
	public static final String SELECT = "select ";
	public static final String FROM = " from ";
	public static final String COUNT = " count(1) ";
	public static final String selectHead = "select * from ";
	public static final String where = " where ";
	public static final String orderBy = " order by ";
	public static final String order_desc = " desc ";
	public static final String order_asc = " asc ";
	public static final String limit = " limit ";
	public static final String update = "update ";
	public static final String and = " and ";
	public static final Pattern pattern = Pattern.compile("(.*)and(\\s)*$");
	
	private Class<T> type;

	private RowMapper<T> rowMapper;
	/**
	 * the key id name
	 */
	private String keyIdName;
	
	private String tableName;
	
	public BaseDao(Class<T> classType) {
		this.type = classType;
		initTableName(classType);
		findKeyId(classType);
		initRowMapper(classType);
		DBMetaInfo.initFields(classType);
	}
	
	private void findKeyId(Class<T> classType) {
		Field[] fields = classType.getDeclaredFields();
		for (Field field : fields) {
			if (field.getAnnotation(Id.class) != null) {
				Column column = field.getAnnotation(Column.class);
				keyIdName = (column == null ? field.getName() : column.name());
				break;
			}
		}
	}
	
	
	private void initTableName(Class<T> classType) {
		this.tableName = classType.getAnnotation(Table.class).name();
	}
	
	private void initRowMapper(Class<T> classType) {
		rowMapper = new ClassAnnotationRowMapper<T>(classType);
	}
	
	public String getNomalSqlHead(String querySql) {
		return new StringBuilder().append(selectHead).append(where).append(" ").append(querySql).toString();
	}

	protected JdbcTemplate jdbcTemplate;
	
	@Resource(name="jdbcTemplate")
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	protected JdbcTemplate getJdbcTemplate(){
		return jdbcTemplate;
	}
	
	public <T> T queryObject(JdbcTemplate jdbcTemplate,String sql, RowMapper<T> rowMapper, Object... objects ) {
		List<T> resultList = jdbcTemplate.query(sql, objects, rowMapper);
		return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
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
			log.info(logSqlFormat(sqlStr, "\\?", argsArr.toArray()));
		}
		return jdbcTemplate.update(sqlStr, argsArr.toArray());
	}

	@Override
	public int insertNotNull(T record) {
		return insert(record);
	}

	@Override
	public int update(T recode) {
		return updateNotNull(recode);
	}

	@Override
	public int updateNotNull(T recode) {
		return updateObj(recode);
	}

	
	@Override
	public List<T> getAll() {
		String sql = SELECT + DBMetaInfo.getFieldsStr(type) + FROM + tableName;
		log.debug(sql);
		List<T> result = jdbcTemplate.query(sql, rowMapper);
		if (log.isDebugEnabled()) {
			log.debug("result.size={}", result.size());
		}
		return result;
	}

	@Override
	public int deleteById(Object key) {
		StringBuilder sql = new StringBuilder("delete from ").append(tableName)
				.append(where).append(keyIdName).append(" = ?");
		log.debug(sql.toString());
		int result = jdbcTemplate.update(sql.toString(), key);
		log.debug("excete.efect.num={}", result);
		return result;
	}

	@Override
	public T selectByKey(Object key) {
		String sql = selectHead + tableName + where + keyIdName + " = ?";
		log.debug(logSqlFormat(sql, key));
		List<T> list = jdbcTemplate.query(sql, rowMapper, key);
		log.debug("result.size={}", list.size());
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	public List<T> select(T record) {
		return null;
	}
	
	@Override
	public List<T> selectByExample(Example example) {
		String columns = example.getSelectProperties();
		columns = StringUtils.isEmpty(columns) ? DBMetaInfo.getFieldsStr(type) : columns;
		String sql = SELECT + columns + FROM + tableName + example.getWhere() + example.getGroupBy()
			+ example.getOrderByString() + example.getPageString();
		if (log.isDebugEnabled()) {
			log.debug(logSqlFormat(sql, example.getValues().toArray()));
		}
		List<T> result = jdbcTemplate.query(sql, example.getValues().toArray(), rowMapper);
		if (log.isDebugEnabled()) {
			log.debug("result.size={}", result.size());
		}
		return result;
	}

	
	@Override
	public int countByExample(Example example) {
		String sql = SELECT + COUNT + FROM + tableName + example.getWhere();
		if (log.isDebugEnabled()) {
			log.debug(logSqlFormat(sql, example.getValues().toArray()));
		}
		Integer result = jdbcTemplate.queryForObject(sql, Integer.class, example.getValues().toArray());
		log.debug("result.data={}", result);
		return result == null ? 0 : result;
	}

	@Override
	public int selectCount(T recode) {
		QuerySqlHelper sqlHelp = getQuerySql(recode);
		String sql = SELECT + "COUNT(1)" + FROM + tableName + where + sqlHelp.getSql();
		if (log.isDebugEnabled()) {
			log.debug(logSqlFormat(sql, sqlHelp.getArgs().toArray()));
		}
		Integer result = jdbcTemplate.queryForObject(sql, Integer.class, sqlHelp.getArgs().toArray());
		log.debug("result.data={}", result);
		return result == null ? 0 : result;
	}

	private UpdateSqlHelper getAddSql(T obj) {
		StringBuilder sbField=new StringBuilder();
		StringBuilder sbArgs=new StringBuilder();
		List<Object> args=new ArrayList<Object>();
		sbField.append(" ( ");
		sbArgs.append(" ( ");
		Field [] fields=obj.getClass().getDeclaredFields();
		if(fields!=null&&fields.length>0){
			for(int i=0;i<fields.length;i++){
				if(Modifier.isStatic(fields[i].getModifiers())) continue;
				String name= fields[i].getName();
				Object value= BeanUtils.getProperty(obj, name);
				if (value == null || fields[i].getAnnotation(Transient.class) != null) {
					continue;
				}
				Column column = AnnontationUtils.getFromField(Column.class, obj.getClass(), name);
				String columnName = column == null ? name : column.name();
				sbField.append(columnName);
				sbField.append(",");
				sbArgs.append("?,");
				args.add(value);
			}
		}
		if(sbField.charAt(sbField.length() - 1) == ','){
			sbField.deleteCharAt(sbField.length()-1);
			sbArgs.deleteCharAt(sbArgs.length() - 1);
		}
		sbField.append(" ) ");
		sbArgs.append(" ) ");
		UpdateSqlHelper sqlHelper = new UpdateSqlHelper();
		sqlHelper.setArgs(args);
		sqlHelper.setFieldSql(sbField);
		sqlHelper.setMarkSql(sbArgs);
		return sqlHelper;
	}
	
	
	/**
	 * 更新对象的所有字段  主键作为更新条件，不更新creatTime字段
	 * @param keyField  数据库主键字段
	 * @param obj
	 */
	private int updateObj(T obj){
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
				if(value == null || field.getAnnotation(Transient.class) != null) continue;
				Column column = AnnontationUtils.getFromField(Column.class, obj.getClass(), name);
				String columnName = (column == null ? name : column.name());
				
				if(!keyIdName.equalsIgnoreCase(columnName)){
					sbField.append(columnName).append(" = ?,");
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
				log.debug(logSqlFormat(sbField.toString(), args.toArray()));
			}
			int result = jdbcTemplate.update(sbField.toString(), args.toArray());
			log.debug("excute.efect.num:{}",result);
			return result;
		}
		return 0;
	}
	/**
	 * @param sqlHelper
	 * @param orderField
	 * @param pageSize
	 * @param pageNo
	 * @return
	 */
	private QuerySqlHelper getQueryFullSql(QuerySqlHelper sqlHelper,String orderField, int pageSize, int pageNo){
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
		}
		if (pageSize > 0 && pageNo > 0) {
			querySql.append(" limit ?,?");
			sqlHelper.getArgs().add((pageNo - 1) * pageSize);
			sqlHelper.getArgs().add(pageSize);
		}
		if (log.isDebugEnabled()) {
			log.debug(logSqlFormat(querySql.toString(), "\\?", sqlHelper.getArgs().toArray()));
		}
		sqlHelper.setSql(querySql);
		return sqlHelper;
	}
	
	public String logSqlFormat(String string, String regex, Object...args){
		try {
			if (args == null || args.length == 0) {
				return string;
			}
			for (Object object : args) {
				string = string.replaceFirst(regex, "\"" + object.toString() + "\"");
			}
			return string;
		} catch (Exception e) {
			return string;
		}
	}
	private String logSqlFormat(String sql, Object...args) {
		return logSqlFormat(sql, "\\?", args);
	}
	@Override
	public List<T> queryList(QueryVo query){
		QuerySqlHelper sqlHelper = getQuerySql(query);
		sqlHelper = getQueryFullSql(sqlHelper, query.getOrderBy(), query.getPageSize(), query.getPageNo());
		StringBuilder querySql = sqlHelper.getSql();
		List<Object> args = sqlHelper.getArgs();
		if (log.isDebugEnabled()) {
			log.debug(logSqlFormat(querySql.toString(), "\\?", args.toArray()));
		}
		return jdbcTemplate.query(querySql.toString(), rowMapper, args.toArray());
	}
	
	/**
	 * 通过queryVo 组装查询的sql 语句
	 * @param queryVo
	 * @return
	 */
	public QuerySqlHelper getQuerySql(Object queryVo) {
		QuerySqlHelper sqlHelper = new QuerySqlHelper();
		StringBuilder sbField=new StringBuilder();
		List<Object> args=new ArrayList<Object>();
		Field [] fields = queryVo.getClass().getDeclaredFields();
		if(fields!=null&&fields.length>0){
			for(Field field : fields){
				String name= field.getName();
				Class<?> type = field.getType();
				if(Modifier.isStatic(field.getModifiers())) continue;
				Object value=BeanUtils.getProperty(queryVo, name);
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
	
}
