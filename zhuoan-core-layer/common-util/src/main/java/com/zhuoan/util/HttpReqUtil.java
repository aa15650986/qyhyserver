
package com.zhuoan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;

public class HttpReqUtil {
	public HttpReqUtil() {
	}

	public static String doGet(String url, String queryString, String charset) {
		StringBuffer resp = new StringBuffer();
		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod(url);

		try {
			if (StringUtils.isNotBlank(queryString)) {
				method.setQueryString(URIUtil.encodeQuery(queryString));
			}

			client.executeMethod(method);
			if (method.getStatusCode() == 200) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(), charset));

				String line;
				while((line = reader.readLine()) != null) {
					resp.append(line);
				}

				reader.close();
			}
		} catch (URIException var12) {
			var12.printStackTrace();
			System.out.println("执行HTTP Get请求时，编码查询字符串“" + queryString + "”发生异常！");
		} catch (IOException var13) {
			var13.printStackTrace();
			System.out.println("执行HTTP Get请求" + url + "时，发生异常！");
		} finally {
			method.releaseConnection();
		}

		return resp.toString();
	}

	public static String doPost(String url, Map<String, String> params, String queryString, String charset) throws UnsupportedEncodingException {
		StringBuffer response = new StringBuffer();
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(url);
		post.getParams().setParameter("http.protocol.content-charset", "utf-8");
		if (params != null) {
			NameValuePair[] p = new NameValuePair[params.size()];
			int i = 0;

			Entry entry;
			for(Iterator var9 = params.entrySet().iterator(); var9.hasNext(); p[i++] = new NameValuePair((String)entry.getKey(), (String)entry.getValue())) {
				entry = (Entry)var9.next();
			}

			post.setRequestBody(p);
		} else if (queryString != null) {
			RequestEntity requestEntity = new StringRequestEntity(queryString, "text/xml", "UTF-8");
			post.setRequestEntity(requestEntity);
		}

		try {
			client.executeMethod(post);
			if (post.getStatusCode() == 200) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(post.getResponseBodyAsStream(), charset));

				String line;
				while((line = reader.readLine()) != null) {
					response.append(line);
				}

				reader.close();
			}
		} catch (IOException var14) {
			System.out.println("执行HTTP Post请求" + url + "时，发生异常！");
		} finally {
			post.releaseConnection();
		}

		return response.toString();
	}
}
