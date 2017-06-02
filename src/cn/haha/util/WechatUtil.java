package cn.haha.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sword.wechat4j.token.TokenProxy;
//import org.sword.wechat4j.token.TokenProxy;

import com.cgt.expand.WeixinOperator;
import com.cgt.utils.HttpUtil;
import com.cgt.utils.SHA1;
import com.webbuilder.common.Var;
import com.webbuilder.utils.DateUtil;
import com.webbuilder.utils.WebUtil;


public class WechatUtil {
	private static Logger logger = Logger.getLogger(WechatUtil.class);
	//private static final String restfulURL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=";
	
	
	
	public static String getSignature(String jsapi_ticket,String noncestr,String timestamp,String url){
		SHA1 s=new SHA1();
		String signature_t="jsapi_ticket="+jsapi_ticket+"&noncestr="+noncestr+"&timestamp="+timestamp+"&url="+url;
		String signature=s.getDigestOfString(signature_t.getBytes());
		return signature;
	}
	
	
	
	
	public static Map<String,Object> getSignature(HttpServletRequest request){
		String url_t = null;
		try {
			url_t = Var.get("cgtsystem.url")+"/main";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StringBuffer url = new StringBuffer() ;
		url.append(url_t);
		 if (request.getQueryString() != null) {
		  url.append("?");
		  url.append(request.getQueryString());
		 }
		//String url="http://17003061990.xicp.net/ss_weixin/main?xwl=23XEQNPQ6BPN";
		Map<String,Object> map=new HashMap<String,Object>();
		String jsapi_ticket=TokenProxy.jsApiTicket();
		String noncestr="weixin_cgt";
		long timestamp_t=Calendar.getInstance().getTimeInMillis();
		String timestamp=timestamp_t+"";
		timestamp=timestamp.substring(0, 10);
		String signature=getSignature(jsapi_ticket,noncestr,timestamp,url.toString());
		map.put("jsapi_ticket", jsapi_ticket);
		map.put("noncestr", noncestr);
		map.put("timestamp", timestamp);
		map.put("signature", signature.toLowerCase());
		map.put("appid", WechatUtil.getAppId());
		return map;
	}
	
	
	
	//map转换为json字符�?
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
	
	
	
	// 发�?模板消息
	public static String  sendTemplateMsg(String toUserOpenid, String tId,Map data) throws Exception{
		return sendTemplateMsg(toUserOpenid, tId, data,null,null);
	} 
	
	
	// 发�?模板消息
	public static String  sendTemplateMsg(String toUserOpenid, String tId,Map data,String url,String topcolor) throws Exception{
		String dataStr="{}";
		if(data!=null)
		{
			dataStr=hashMapToJson(data);
		}
		
		if(topcolor==null)
		{
			topcolor="#000000";
		}
		
		
		//System.out.println("========================="+jo.toString(1));
		String jsonStr=  "{\"touser\":\"" + toUserOpenid +"\"," +
				"\"template_id\":\"" + tId + "\"," ;
		if(url!=null&& !"".equals(url))
		{
			jsonStr+="\"url\":\"" + url + "\"," ;
		}
		jsonStr+="\"topcolor\":\""+topcolor+"\"";
		if (data != null && !"".equals(data)) {
			jsonStr += ",\"data\":" + dataStr + "}"; 
		}
		
		WeixinOperator wo=new  WeixinOperator();
		wo.xwlObject=new JSONObject();
		wo.xwlObject.put("type","/message/template/send");
		wo.xwlObject.put("params",jsonStr);
		wo.create();
		return wo.result;
	} 
	
	
		
	// 群发消息
	public static String  sendAllMsg(String templateId,Map dataMap,String url,String topcolor) throws Exception{
		JSONArray ids=getAllUserIds();
		if(ids!=null)
		{
			for (int i=0;i<ids.length();i++) {
				String id=(String)ids.get(i);
				sendTemplateMsg(id, templateId, dataMap,url,topcolor);
			}
		}
		
		return null;
	} 
		
	public static JSONObject uploadNews(JSONArray articles) throws Exception
	{
		WeixinOperator wo=new WeixinOperator();
		wo.xwlObject=new JSONObject();
		wo.xwlObject.put("type", "/media/uploadnews");
		JSONObject params=new JSONObject();
		params.put("articles", articles);
		String param = params.toString();
		System.out.println("param"+param);
		wo.xwlObject.put("params",param);		
		wo.create();
		return wo.resultObj;		
	}
	
		// 群发消息
		public static String  sendAllMsg(HttpServletRequest request,HttpServletResponse response) throws Exception{
			String templateId=(String) request.getAttribute("wx_templateId");
			Map dataMap=(Map) request.getAttribute("wx_dataMap");
			String url=(String) request.getAttribute("wx_url");
			String topcolor=(String) request.getAttribute("wx_topcolor");
			JSONArray ids=getAllUserIds();
			if(ids!=null)
			{
				for (int i=0;i<ids.length();i++) {
					String id=(String)ids.get(i);
					sendTemplateMsg(id, templateId, dataMap,url,topcolor);
				}
			}
			
			return null;
		} 	
	
	private static JSONArray getAllUserIds() throws Exception {
		String type="/user/get";
		WeixinOperator wo=new  WeixinOperator();
		wo.xwlObject=new JSONObject();
		wo.xwlObject.put("type",type);
		wo.create();
		JSONObject jo=new JSONObject(wo.result);
		JSONObject data=(JSONObject)jo.opt("data");
		JSONArray openid=(JSONArray)data.opt("openid");
		
		System.out.println(openid.toString());
		
		return openid;
	}

	public static String getFullUrl(HttpServletRequest request,String url) throws UnsupportedEncodingException{
		
		
		String basePath="";
		try {
			basePath = Var.get("cgtsystem.url");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//String basePath = "http://cgt.vicp.net:8787/"+ path + "/";
		//basePath=basePath.replace(":80/", "/");
		try {
			url= URLEncoder.encode(basePath+"/"+url, "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String authUrl="";
		authUrl="https://open.weixin.qq.com/connect/oauth2/authorize?appid="+WechatUtil.getAppId()+"&redirect_uri="+url+"&response_type=code&scope=snsapi_base&state=CGT001#wechat_redirect";
		System.out.println("authUrl:"+authUrl);
		return authUrl;
	}

	public static String getAppId(){
		String appid=org.sword.wechat4j.common.Config.instance().getAppid();
		return appid;
	}
	
	/**
	 * 根据请求得到openId并设置在会话�?
	 * 尽量在菜单的首页面使�?
	 * @param request
	 * @throws Exception 
	 */
	public static void setOpenId(HttpServletRequest request) throws Exception{
		HttpSession session=request.getSession();
		String openId=(String) session.getAttribute("wx_openid");
		System.out.println("---------session wx_openid:"+openId);
		if(openId==null||openId.equals(""))
		{
			String code = WebUtil.fetch(request,"code") ;
			if(code!=null&&!code.equals(""))
			{
				System.out.println("---------------网页授权code:"+code);
				openId = WechatUtil.getWxOpenidByCode(code);
				System.out.println("openid:"+openId);
				System.out.println("---------------get openid:"+code);
				session.setAttribute("wx_openid",openId);
			}
		}
	} 
	
	/**
	 * 根据请求得到openId并设置在会话�?
	 * 尽量在菜单的首页面使�?
	 * @param request
	 * @throws Exception 
	 */
	public static void setOpenId(HttpServletRequest request,HttpServletResponse response) throws Exception{
		HttpSession session=request.getSession();
		String openId=(String) session.getAttribute("wx_openid");
		System.out.println("---------session wx_openid:"+openId);
		if(openId==null||openId.equals(""))
		{
			String code = WebUtil.fetch(request,"code") ;
			if(code!=null&&!code.equals(""))
			{
				System.out.println("---------------网页授权code:"+code);
				openId = WechatUtil.getWxOpenidByCode(code);
				System.out.println("openid:"+openId);
				System.out.println("---------------get openid:"+code);
				session.setAttribute("wx_openid",openId);
			}
			else{
				//构�?当前的url
				String url = null;
				//构�?网页权限的url
				String fullUrl=getFullUrl( url);
				request.getRequestDispatcher(fullUrl).forward(request,response);
				return;
			}
		}
	} 
	
	/**
	 * 得到设置在会话中的openId
	 * 大部分在数据保存的时候使�?
	 * @param request
	 * @return
	 */
	public static String getOpenId(HttpServletRequest request){
		HttpSession session=request.getSession();
		String openId = (String) session.getAttribute("wx_openid");
		return openId;
	} 
	
	
	public static String getWxOpenidByCode(String code) throws Exception{
		String appid=org.sword.wechat4j.common.Config.instance().getAppid();
		String secret=org.sword.wechat4j.common.Config.instance().getAppSecret();
		String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="+appid+"&secret="+secret+"&code="+code+"&grant_type=authorization_code";
		String result  = httpGet(url);
		
		if(result==null||result.equals(""))
		{
			throw new Exception("获取网页授权access_token失败!");
		}
		System.out.println("------------网页授权access_token:"+result);
		String openid ="";
		int flag =  result.indexOf("openid");
		if(flag != -1)
		try {
			JSONObject json = new JSONObject(result);
			openid = json.getString("openid");
			return openid;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//字段值不空才去URLEncoder
	private static String encodeArb(String fieldValue){
		if(StringUtils.isNotBlank(fieldValue)){
			try {
				fieldValue = URLEncoder.encode(fieldValue,"utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return fieldValue;
		}else{
			return null;
		}
		
	}
	
	//发�?httpget请求
	private static String httpGet(String url) {
		try {
			HttpEntity entity = Request.Get(url).execute().returnResponse().getEntity();
			return entity != null ? EntityUtils.toString(entity) : null;
		} catch (Exception e) {
			logger.error("get请求异常�? + e.getMessage() + "\n get url:" + url);
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	
	
	public static void main(String[] args) throws Exception {
		//basicM();
	
		//getAllUserIds();
		
		//testAllNotice();
		
		//testGetFullURL();
		//testReplyNotice();
		
		//testUpload();
		//testAllSend();
		
		//test:JSONObject uo=getUserInfoFromOpenId("o3PM-tw8_kQzg9NY9kNQfpq7KtQQ");
		//real:JSONObject uo=getUserInfoFromOpenId("oUycXs7XDiF-0Yw9bO7_gP8dkLk0");
		//b158://oUycXs1U82ODKNYHGv9z3EWQR8jk  //oUycXs0uwvhLlrswrtdWHO5OCby4//oUycXsw0ZsqkkwsR9616nFNpWpmU//oUycXs-aUYHKX5NoRlsFmnCBqv1s
		// {"tagid_list":[],"subscribe":0,"openid":"oUycXs0uwvhLlrswrtdWHO5OCby4"}

		JSONObject uo=getUserInfoFromOpenId("oUycXs1U82ODKNYHGv9z3EWQR8jk");//oUycXs7-sx-KBjbc5y75KWP2jEm0
		//
		java.util.Date d=new java.util.Date();
		 d.setTime(new Long("1466304703000"));//14792 72998 494
		System.out.println(d.getTime());
		System.out.println(DateUtil.getDateString(d));
		System.out.println(uo);
	}

	public static void testUpload() throws JSONException, Exception {
		JSONArray articles=new JSONArray();
		JSONObject art=new JSONObject();
		art.put("thumb_media_id","d5jG-3eYpnFI4I1c8yDbssJl1Rb3Smy2lJjssl9_xUe1BWtm0Q1PgpJnIMspjTyr");
		art.put("author","xxx");
		art.put("title","Happy Day");
		art.put("content_source_url","www.qq.com");
		art.put("url","www.qq.com");
		art.put("content","<a href=\"www.qq.com\">213123 content</a><div style=\"font-size:17px\" onclick=\"alert(122)\">1212123ads</div>");
		art.put("digest","digest");
		art.put("show_cover_pic",1);
		articles.put(art);
		JSONObject art2=new JSONObject();
		art2.put("thumb_media_id","d5jG-3eYpnFI4I1c8yDbssJl1Rb3Smy2lJjssl9_xUe1BWtm0Q1PgpJnIMspjTyr");
		art2.put("author","xxx");
		art2.put("title","Happy Day2");
		art2.put("content_source_url","www.qq.com");
		art2.put("content","1231233<script>alert(121)</script>");
		art2.put("digest","digest");
		art2.put("show_cover_pic",1);
		articles.put(art2);
		
		
		
		uploadNews(articles);
		
	}
	private static void testAllSend() throws JSONException, Exception {
		
		//exv8r1hY1pPYnwsRKMX2u_YY4JtsvEZyv_VdLghzyYTHNPo_ZpL9leVuQB8rMy_K
		JSONObject jo=new JSONObject(); 
		JSONArray touser=new JSONArray();
		touser.put("oUycXs8PvVnt5udKDIXkfthlq1pA");//张劲
		touser.put("oUycXs7XDiF-0Yw9bO7_gP8dkLk0");//�?
		touser.put("oUycXs5Mi0_q1lgoXS7IyAUP2dSo");//�?
		
		JSONObject mpnews=new JSONObject();
		mpnews.put("media_id", "L53f9oQz-8Svt5JKx7nqsB9pJJut13y-ldSmn3HBoGHbA5JccgMEqtErObrlfH5_");
		//mpnews.put("media_id", "exv8r1hY1pPYnwsRKMX2u_YY4JtsvEZyv_VdLghzyYTHNPo_ZpL9leVuQB8rMy_K");
		jo.put("touser", touser);
		jo.put("mpnews", mpnews);
		jo.put("msgtype", "mpnews");
		sendTuWen(jo);
		
	}



	private static JSONObject sendTuWen(JSONObject jo) throws Exception {
		WeixinOperator wo=new WeixinOperator();
		wo.xwlObject=new JSONObject();
		wo.xwlObject.put("type", "/message/mass/send");
		//JSONObject params=new JSONObject();
		String param = jo.toString();
		System.out.println("param"+param);
		wo.xwlObject.put("params",param);		
		wo.create();
		return wo.resultObj;	
	}

	private static JSONObject getUserInfoFromOpenId(String openId) throws Exception {
		String token = TokenProxy.accessToken();
		String url="https://api.weixin.qq.com/cgi-bin/user/info?access_token="+token;
		url+="&openid="+openId;
		url+="&lang=zh_CN";
		
		System.out.println(url);
		
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url);
		
		String result=null;
		JSONObject resultObj=null;
		HttpResponse httpResponse = httpClient.execute(httpPost);
		HttpEntity entity1 = httpResponse.getEntity();
		if(entity1!=null)
		{
			result=EntityUtils.toString(entity1, "UTF-8");
		}
		if(result!=null)
		{
			resultObj=new JSONObject(result);
		}
		
		/*
		WeixinOperator wo=new WeixinOperator();
		wo.xwlObject=new JSONObject();
		wo.xwlObject.put("type", "/user/info");
		//JSONObject params=new JSONObject();
		String param = openId;
		//System.out.println("param"+param);
		wo.xwlObject.put("openid",param);
		wo.xwlObject.put("lang","zh_CN");
		wo.create();
		return wo.resultObj;*/
		return resultObj;
	}
	
	private static void testAllNotice() throws Exception {
		//活动公告通知	{{first.DATA}}活动名称：{{keyword1.DATA}}公告类型：{{keyword2.DATA}}{{remark.DATA}}
		HashMap<String, Object> hm = new HashMap();   
		hm.put("first", new DTO("我市将举行�?新环保法》的意见征集，取纳�?将获得XXXX!","#173177"));
		hm.put("keyword1", new DTO("《新环保法�?的意见征�?));
		hm.put("keyword2", new DTO("意见征集"));
		sendAllMsg("SzNkxlpQfggfJwabXl2fVKiWfXVonrRm-LMfC7iizb8", hm, null, null);
	}



	private static void testReplyNotice() throws Exception {
		//{{first.DATA}}回复者：{{keyword1.DATA}}回复时间：{{keyword2.DATA}}回复内容：{{keyword3.DATA}}{{remark.DATA}}�?
		HashMap<String, Object> hm = new HashMap();   
		hm.put("first", new DTO("您好，您关于《XXX》的咨询已经得回�?,"#173177"));
		hm.put("keyword1", new DTO("微信平台"));
		hm.put("keyword2", new DTO("2015-6-2 10:00"));
		hm.put("keyword3", new DTO("您好,YYYYYjlkjlksjdlsjdlksjd\\n skjdlsjdldk"));
		sendTemplateMsg( "o3PM-tw8_kQzg9NY9kNQfpq7KtQQ", "RKkvYBep7WSLh225OGV0PlzZBC73ZPZJU12AWwSg1eg",hm);
	}


	private static void basicM() throws IOException, ClientProtocolException {
		HashMap<String, Object> hm = new HashMap();   
		hm.put("first", new DTO("12211122","#173177"));
		hm.put("keyword1", new DTO("2","#173177"));
		hm.put("keyword2", new DTO("3","#000000"));
		
		//hm.put("", new DTO("",""));
		hm.put("remark", new DTO("7 ","#000000"));
		try {
			sendTemplateMsg("o3PM-tw8_kQzg9NY9kNQfpq7KtQQ", "EGMY038_7x-4z7o5cutIMjdS1HEmPV9xdp_9biKDTV4", null, null,null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static String getFullUrl(String path) throws UnsupportedEncodingException {
		return getFullUrl(null, path);
	}

	public static String getAuthUrl(String url) throws UnsupportedEncodingException {
		String authUrl="https://open.weixin.qq.com/connect/oauth2/authorize?appid="+WechatUtil.getAppId()+"&redirect_uri={?redirect_uri?}&response_type=code&scope=snsapi_base&state=CGT001#wechat_redirect";
		String changeUrl=authUrl.replace("{?redirect_uri?}", URLEncoder.encode(url, "utf-8"));
		return changeUrl;
		
	}
	
	
	public static void testGetFullURL() {
		String url="http://ymsgzh.chancheng.gov.cn/main?xwl=23ZM79QMK5YN&street=zm&number=F001";
		try {
			url= URLEncoder.encode(url, "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String authUrl="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxf93565e19dd64b08&redirect_uri="+url+"&response_type=code&scope=snsapi_base&state=CGT001#wechat_redirect";
		System.out.println("authUrl:"+authUrl);
	}
	
	/*微信支付*/
	
	public static String getPaySign(String prepay_id,String noncestr,String timestamp){
		String paySign = "appId="+WechatUtil.getAppId()+"&timestamp="+timestamp+"&nonceStr="+noncestr+"&package="
				+prepay_id+"&signType=MD5";
		
		String signature=MD5(paySign);
		return signature;
	}

	
	public static SortedMap<String,String> getPaySignature(HttpServletRequest request) throws Exception{	
		
		String noncestr="weixin_grt";
		long timestamp_t=Calendar.getInstance().getTimeInMillis();
		String timestamp=timestamp_t+"";
		timestamp = timestamp.substring(0, 10);
		String prepay_id = getPayList(request);
		SortedMap<String, String> map = new TreeMap<String, String>();
		map.put("appId", WechatUtil.getAppId());
		map.put("timeStamp", timestamp);
		map.put("nonceStr", noncestr);
		map.put("package", prepay_id);
		map.put("signType", "MD5");
		String paySign = getSign(map);
		map.put("paySign", paySign);
		System.out.println("传�?的map�?"+map);
		return map;
	}
	
	
	public static String getPayList(HttpServletRequest request) throws Exception{
		
		String address = "https://api.mch.weixin.qq.com/pay/unifiedorder";
		
		String appid = WechatUtil.getAppId().toString();
		String mch_id = "1317773401";
		String noncestr="weixin_grt";
		String body = "test";
		Date d = new Date();
		String out_trade_no = getOutTradeNo();
		String total_fee = "1";
		String spbill_create_ip ="59.39.58.105";
		String notify_url = "http://testgzh2.chancheng.gov.cn/gr";
		String trade_type = "JSAPI";
		
		SortedMap<String, String> params = new TreeMap<String, String>();
        params.put("appid", appid);
        params.put("mch_id", mch_id);
        params.put("nonce_str", noncestr);
        params.put("body", body);//商品描述
        params.put("spbill_create_ip", spbill_create_ip);
        params.put("out_trade_no", out_trade_no);
        params.put("total_fee", total_fee);
        params.put("trade_type", trade_type);
        params.put("notify_url", notify_url);
        //params.put("openid", "ocfQot2XGpUMEV6inU6jlNxZVRfs"); 
        params.put("openid", getOpenId(request));
        //openid:必须先关�?ocfQot2XGpUMEV6inU6jlNxZVRfs
       // System.out.println(getOpenId(request).toString());
        String sign = getSign(params);
         
        String res = HttpUtil.post4(address, parseString2Xml(params,sign));
        //String res2 = HttpUtil.post2(address, jo);
        System.out.println(res);
        
        Map<String,Object> resMap = parseXML2Map(res);
        String prepay_id = resMap.get("prepay_id").toString();
		prepay_id = prepay_id.substring(1, prepay_id.length());
		prepay_id = prepay_id.substring(0, prepay_id.length()-1);
		System.out.println(prepay_id);
        return  prepay_id;
	}
	
	
	public static String getOutTradeNo(){
		 Date currentTime = new Date();
		   SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		   String dateString = formatter.format(currentTime);
		   return dateString;
	}
	
	public static String parseString2Xml(Map<String, String> map,String sign){
        StringBuffer sb = new StringBuffer();
        sb.append("<xml>");
        Set es = map.entrySet();
        Iterator iterator = es.iterator();
        while(iterator.hasNext()){
            Map.Entry entry = (Map.Entry)iterator.next();
            String k = (String)entry.getKey();
            String v = (String)entry.getValue();
            sb.append("<"+k+">"+v+"</"+k+">");
        }
        sb.append("<sign>"+sign+"</sign>");
        sb.append("</xml>");
        System.out.println(sb.toString());
        return sb.toString();
    }
	
	public static Map<String,Object> parseXML2Map(String xmlString) throws Exception{
		Document doc = DocumentHelper.parseText(xmlString);  
		  Element rootElement = doc.getRootElement();  
		  Map<String, Object> map = new HashMap<String, Object>();  
		  ele2map(map, rootElement);  
		  System.out.println(map);  
		  // 到此xml2map完成，下面的代码是将map转成了json用来观察我们的xml2map转换的是否ok  
		  //String string = JSONObject.fromObject(map).toString();  
		  //System.out.println(string);  
		  return map;  
		
    }
	
	
	
	public static void ele2map(Map map, Element ele) {  
		  System.out.println(ele);  
		  // 获得当前节点的子节点  
		  List<Element> elements = ele.elements();  
		  if (elements.size() == 0) {  
		   // 没有子节点说明当前节点是叶子节点，直接取值即�? 
		   map.put(ele.getName(), ele.getText());  
		  } else if (elements.size() == 1) {  
		   // 只有�?��子节点说明不用�?虑list的情况，直接继续递归即可  
		   Map<String, Object> tempMap = new HashMap<String, Object>();  
		   ele2map(tempMap, elements.get(0));  
		   map.put(ele.getName(), tempMap);  
		  } else {  
		   // 多个子节点的话就得�?虑list的情况了，比如多个子节点有节点名称相同的  
		   // 构�?�?��map用来去重  
		   Map<String, Object> tempMap = new HashMap<String, Object>();  
		   for (Element element : elements) {  
		    tempMap.put(element.getName(), null);  
		   }  
		   Set<String> keySet = tempMap.keySet();  
		   for (String string : keySet) {  
		    Namespace namespace = elements.get(0).getNamespace();  
		    List<Element> elements2 = ele.elements(new QName(string,namespace));  
		    // 如果同名的数目大�?则表示要构建list  
		    if (elements2.size() > 1) {  
		     List<Map> list = new ArrayList<Map>();  
		     for (Element element : elements2) {  
		      Map<String, Object> tempMap1 = new HashMap<String, Object>();  
		      ele2map(tempMap1, element);  
		      list.add(tempMap1);  
		     }  
		     map.put(string, list);  
		    } else {  
		     // 同名的数量不大于1则直接�?归去  
		     Map<String, Object> tempMap1 = new HashMap<String, Object>();  
		     ele2map(tempMap1, elements2.get(0));  
		     map.put(string, tempMap1);  
		    }  
		   }  
		  }  
		 }  
	
	
	 
	/**
     * 获取签名 md5加密(微信支付必须用MD5加密)
     * 获取支付签名
     * @param params
     * @return
     */
    public static String getSign(SortedMap<String, String> params){
    	/*
		String sign = "appid="+appid+"&mch_id="+mch_id
				+"&noncestr="+noncestr+"&body="+body+"&out_trade_no="+out_trade_no
				+"&total_fee="+total_fee+"&spbill_create_ip="+spbill_create_ip
				+"notify_url"+notify_url+"trade_type"+trade_type;
		sign = MD5(sign).toUpperCase();
		*/
        String sign = null;
        StringBuffer sb = new StringBuffer();
        Set es = params.entrySet();
        Iterator iterator = es.iterator();
        while(iterator.hasNext()){
            Map.Entry entry = (Map.Entry)iterator.next();
            String k = (String)entry.getKey();
            String v = (String)entry.getValue();
            if (null != v && !"".equals(v) && !"sign".equals(k)&& !"key".equals(k)) {
                sb.append(k+"="+v+"&");
            }
        }
        
        sb.append("key=3B65CB4B1923D6AABA18599D5D05F393");
    	System.out.println("签名传输数据:"+sb.toString());
        sign = MD5(sb.toString()).toUpperCase();
      
        return sign;
    }
	
    public static String getAPIKey(){
    	String s = "guangrongtong";
    	s = MD5(s).toUpperCase();
    	System.out.println(s);
    	//3B65CB4B1923D6AABA18599D5D05F393
    	return s;
    }
	
	
	/**
	 * MD5加密算法
	 * @param sourceStr
	 * @return
	 */
	public static String MD5(String sourceStr) {
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0) 
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
            //System.out.println("MD5(" + sourceStr + ",32) = " + result);
            //System.out.println("MD5(" + sourceStr + ",16) = " + buf.toString().substring(8, 24));
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e);
        }
        return result;
    }
	
	
}
