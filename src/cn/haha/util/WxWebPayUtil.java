package cn.haha.util;

import java.util.Iterator;
import java.util.Map;

public class WxWebPayUtil {

	public WxWebPayUtil() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * mapת��JSON�ַ���
	 * @param map
	 * @return
	 */
	public static String hashMapToJson(Map<String, Object> map) {  
	    String string = "{"; 
	   
	    for (Iterator it = map.entrySet().iterator(); it.hasNext();) {  
	    	Map.Entry e = (Map.Entry) it.next();  
	        string += "\"" + e.getKey() + "\":";  
	        string += "{" + e.getValue() + "},";  
	    }  
	    string = string.substring(0, string.lastIndexOf(","));  
	    string += "}";  
	    return string;    
	}  
	
}
