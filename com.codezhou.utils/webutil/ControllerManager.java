package com.codezhou.utils.webutil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ControllerManager {
    /**
     什么样的路径对应什么样的方法
     *
     */
    private static Map<String, Method> getMap = new HashMap<String, Method>();
    private static Map<String,Class> classMap = new HashMap<String, Class>();//通过path找到Class
    private static Map<Class,Object> objectMap = new HashMap<Class, Object>();//通过class获取这个类的对象

    //加载所有被controller注解标注的类
    static{
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> urls = classLoader.getResources("");
            while(urls.hasMoreElements()){
                URL url = urls.nextElement();
                File file = new File(url.getPath());
                if(file.isDirectory()){
                    File[] files = file.listFiles();
                    for(File temp : files) {
//                分析路径下所有的文件,进入深度优先搜索
                        analysisPackage(temp, "");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Class loaderClass(String className) throws ClassNotFoundException {
        Class clazz = Class.forName(className);
        return clazz;
    }

    //传什么包名分析什么文件
    private static void analysisPackage(File file,String packageName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if(file == null){return;}
        if(file.isDirectory()){
            File[] files = file.listFiles();
            //向下递归
            for(File temp : files){
                analysisPackage(temp,packageName + file.getName()+".");
            }
            //false代表是一个文件,前提是一个.class文件,如何判断他是一个class文件
        }else{
            if(file.getName().endsWith(".class")){
                //全名
                String classFullName = packageName + file.getName();
                //截取.class
                classFullName = classFullName.substring(0,classFullName.lastIndexOf("."));
                Class clazz = loaderClass(classFullName);
                Controller controller = (Controller) clazz.getAnnotation(Controller.class);
                if(controller != null){
                    Method[] methods = clazz.getDeclaredMethods();
                    for(Method method : methods){
                        Path path = method.getAnnotation(Path.class);
                        if(path == null){continue;}
                        getMap.put(path.value(),method);
                        classMap.put(path.value(),clazz);
                        Object obj = objectMap.get(clazz);
                        if(obj == null){
                            obj = clazz.newInstance();
                            objectMap.put(clazz,obj);
                        }
                    }
                }
            }
        }
    }

    /**
     * 返回哪个请求该执行哪个方法
     * 希望静态代码块进行初始化，获取这个方法
     */
    public static void doMethod(HttpServletRequest request, HttpServletResponse response) throws InvocationTargetException, IllegalAccessException {
        Method method = getMap.get(request.getServletPath());
        if(method != null){
            method.setAccessible(true);
            Class clazz = classMap.get(request.getServletPath());
            Object obj = objectMap.get(clazz);
            method.invoke(obj,request,response);
        }else{
            //没有这个方法则返回404
            System.out.println("没有方法");
        }
    }
}
