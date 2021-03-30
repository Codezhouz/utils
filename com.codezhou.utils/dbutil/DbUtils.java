package com.codezhou.utils.dbutil;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.哪个domain对应哪张表
 * 2.对象中的属性对应表中的字段
 *
 * @author Administratoc
 */

public class DbUtils {

    public static final String URL = "jdbc:mysql://localhost:3306/bookmall?characterEncoding=utf-8&amp;serverTimezone=UTC&amp;useSSL=false";
    public static final String userName = "root";
    public static final String passWord = "root";

    //加载驱动类
    static{
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //获取connection
    private static  Connection getConnection() throws SQLException {
        return  DriverManager.getConnection(URL,userName,passWord);
    }



    //创建后返回主键
    public static int insert(Object object) throws Exception{//传递需要操作的表格对应的domain对象
        Table table = object.getClass().getAnnotation(Table.class);
        if (table == null) {
            throw  new Exception("没有这个表格的注解");
        }
        String tableName = table.value();
        StringBuilder columns = new StringBuilder("");
        StringBuilder values = new StringBuilder("");
        Field[] fields = object.getClass().getDeclaredFields();
        for(Field field : fields){
            field.setAccessible(true);
            Column column = field.getAnnotation(Column.class);
            if (column == null) {
                continue;
            }
            //注入值的时候跳过id
            Id id = field.getAnnotation(Id.class);
            if (id != null){
                continue;
            }
            //对除ID之后的属性赋值
            String columnName = column.value();
            Object value = field.get(object);
            columns.append("`").append(columnName).append("`,");
            if (value.getClass() == String.class){
                values.append("\"").append(value).append("\",");
            }else{
                values.append(value).append(",");
            }
        }
        //然后sql语句对应这个表,values也对应属性,这个表中所有列,,,把值都传到对应的列中
        String sql = "insert into " +tableName+ "("+columns.toString().substring(0,columns.length()-1)+")values(" +values.toString().substring(0,values.length()-1)+");" ;
        System.out.println(sql);
        Connection conn = getConnection();
        PreparedStatement preparedStatement = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
        preparedStatement.execute();
        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
        int generateKey = -1;
        while (generatedKeys.next()) {
            generateKey = generatedKeys.getInt(1);
        }
        return generateKey;
    }

//=========================================================================


    private static <T> List<T> getObject(Class<T> clazz,ResultSet resultSet) throws SQLException, IllegalAccessException, InstantiationException {
        List<T> resultList = new ArrayList<T>();
        while(resultSet.next()){
            T obj = clazz.newInstance();
            Field[] fields = clazz.getDeclaredFields();
            for(int i = 0 ; i<fields.length ; i++){
                fields[i].setAccessible(true);
                Column column = fields[i].getAnnotation(Column.class);
                if(column == null){continue;}
                Object value = resultSet.getObject(column.value());
                fields[i].set(obj,value);
            }
            resultList.add(obj);
        }
        return resultList;
    }
//==================================================================================================

    private static void valuation(PreparedStatement preparedStatement,Object...params) throws SQLException {
        for (int i = 0; params != null && i < params.length; i++) {
            if (params[i].getClass() == int.class || params[i].getClass() == Integer.class) {
                preparedStatement.setInt(i + 1, ((Number) params[i]).intValue());
            } else if (params[i].getClass() == long.class || params[i].getClass() == Long.class) {
                preparedStatement.setLong(i + 1, ((Number) params[i]).longValue());
            } else if (params[i].getClass() == float.class || params[i].getClass() == Float.class) {
                preparedStatement.setFloat(i + 1, ((Number) params[i]).floatValue());
            } else if (params[i].getClass() == double.class || params[i].getClass() == Double.class) {
                preparedStatement.setDouble(i + 1, ((Number) params[i]).doubleValue());
            } else if (params[i].getClass() == String.class) {
                preparedStatement.setString(i + 1, (String) params[i]);
            } else if (params[i].getClass() == Date.class) {
                preparedStatement.setDate(i + 1, (Date) params[i]);
            }
        }
    }

//=======================================================================================================================
    //查询一共有多少条数据
    public static int selectCount(Class clazz,String where,Object...params) throws Exception {
        Table table = (Table)clazz.getAnnotation(Table.class);
        if(table == null ){throw new Exception("没有这个表格的注解");}
        String tableName = table.value();
        String sql = "select count(1) as count from"+tableName;
        //判断有没有where条件
        if(where != null && !"".equals(where)){
            sql += " where "+where;
        }else {
            sql += ";";
        }
        Connection conn = getConnection();
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        valuation(preparedStatement,params);
        ResultSet rs = preparedStatement.executeQuery();
        while(rs.next()){
            return rs.getInt("count");
        }
        return 0;
    }

    public static <T> List<T> selectAll(Class<T> clazz) throws Exception {
        return select(clazz," * ",null,null);
    }
    public static <T> List<T> select(Class<T> clazz,String where,Object...params) throws Exception {
        return select(clazz," * ",where,params);
    }
    public static <T> List<T> select(Class<T> clazz,String select,String where,Object...params) throws Exception {
        return select(clazz,select,where,null,null,params);
    }
    public static <T> List<T> select(Class<T> clazz,String select,String where,String orderBy,String limit,Object...params) throws Exception {
        Table table = clazz.getAnnotation(Table.class);
        if (table == null){
            throw new Exception("没有这个表格的注解");
        }
        String tableName = table.value();
        //拼接sql语句
        StringBuilder sql = new StringBuilder("select " + select + " from " + tableName);
        //将sql条件拼接进sql where orderBy  limit
        if(where != null && !"".equals(where)){
            sql.append(" where "+where);
        }
        if(orderBy != null && !"".equals(orderBy)){
            sql.append(" order by "+orderBy);
        }
        if(limit !=null && !"".equals(limit)){
            sql.append(" limit "+limit);
        }
        sql.append(";");
        return selectBySql(clazz,sql.toString(),params);
    }

    public static <T> List<T> selectBySql(Class<T> clazz,String sql,Object...params) throws SQLException, InstantiationException, IllegalAccessException {
        Connection conn =  getConnection();
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        valuation(preparedStatement,params);
        ResultSet rs = preparedStatement.executeQuery();
        return getObject(clazz,rs);
    }

    //=======================================================================================================

    /**
     * sql语句中带？，传递参数修改
     */
    public static <T> void update(Class<T> clazz,String[] keys,Object[] values,String where,Object...params) throws Exception {
        Table table = clazz.getAnnotation(Table.class);
        if(table == null){
            throw new Exception("没有这个表格的注解");
        }
        String tableName = table.value();
        StringBuilder sql = new StringBuilder("update "+tableName+" set ");
        for(int i = 0 ; i<keys.length ; i++){
            sql.append(keys[i]+" = ");
            if(values[i].getClass() == String.class){
                sql.append("'"+values[i]+"',");
            }else{
                sql.append(values[i] + ",");
            }
        }
        //截取获得最终的sql
        String realSql = sql.substring(0,sql.length()-1);
        if(where != null && !"".equals(where)){
            realSql += " where "+ where;
        }
        realSql += ";";
        Connection conn = getConnection();
        PreparedStatement preparedStatement = conn.prepareStatement(realSql);
        for(int i = 0; params!=null && i<params.length; i++){
            preparedStatement.setObject(i+1,params[i]);
        }
        preparedStatement.execute();
    }
    /**
     * 指定修改哪些数据
     * @param <T>
     */
    public static <T> void update(Class<T> clazz,String[] keys,Object[] values,String where) throws Exception {
        update(clazz,keys,values,where,null);
    }

    /**
     * 根据主键进行修改
     * @param obj
     * @param <T>
     */
    public static <T> void update(T obj) throws Exception {
        String primkey = "";
        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field : fields){
            field.setAccessible(true);
            Id id = field.getAnnotation(Id.class);
            if(id != null){
                Column column = field.getAnnotation(Column.class);
                primkey += column.value()+ " = " + field.get(obj);
            }
            if(primkey != null && !"".equals(primkey)){
                update(obj,primkey);
            }else{
                throw new Exception("主键错误,不能执行update操作");
            }
        }
    }
    /**
     * <T> 的意思是约束后面Class<T>
     * @param <T>
     */
    public static <T> void update(T obj,String where) throws Exception {
        Table table = obj.getClass().getAnnotation(Table.class);
        if(table == null){
            throw new Exception("没有这个table的注解");
        }
        String tableName = table.value();
        StringBuilder sql = new StringBuilder("update "+tableName + " set ");
        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field : fields){
            field.setAccessible(true);
            Column column = field.getAnnotation(Column.class);
            if(column == null){continue;}
            Id id = field.getAnnotation(Id.class);
            if(id != null){continue;}
            sql.append(column.value()).append(" = ");
            if(field.getType() == String.class){
                sql.append("'"+field.get(obj)+"',");
            }else {
                sql.append(field.get(obj)).append(",");
            }
        }
         String realsql = sql.substring(0,sql.length()-1);
        //拼接where条件
        if(where != null){
            realsql += " where "+where;
        }
        realsql += ";";
        Connection conn = getConnection();
        PreparedStatement preparedStatement = conn.prepareStatement(realsql);
        preparedStatement.execute();
    }

//============================================================================================

    /**
     * 根据主键删数据
     * @param <T>
     */
    public static <T> void delete(T obj) throws Exception {
        String primKey = "";
        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field : fields){
            field.setAccessible(true);
            Id id = field.getAnnotation(Id.class);
            if(id != null){
                Column column = field.getAnnotation(Column.class);
                primKey += column.value() + " = " + field.get(obj);
            }
        }
        if(primKey != null && !"".equals(primKey)){
            delete(obj.getClass(),primKey);
        }else{
            throw new Exception("主键错误，无法执行delete操作");
        }
    }

    /**
     * 根据where条件删数据
     */
    public static <T> void delete(Class<T> clazz,String where) throws Exception {
        delete(clazz,where,null);
    }

    /**
     * 根据带?参数删除数据
     */
    public static <T> void delete(Class<T> clazz,String where,Object...params) throws Exception {
        Table table = clazz.getAnnotation(Table.class);
        if(table == null){
            throw new Exception("没有这个表格的注解");
        }
        String tableName = table.value();
        StringBuilder sql = new StringBuilder("delete from "+tableName +" ");
        if(where != null){
            sql.append(" where "+ where);
        }
        sql.append(";");
        Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());
        for(int i = 0; params != null && i<params.length; i++){
            preparedStatement.setObject(i+1,params[i]);
        }
        preparedStatement.execute();
    }
}
