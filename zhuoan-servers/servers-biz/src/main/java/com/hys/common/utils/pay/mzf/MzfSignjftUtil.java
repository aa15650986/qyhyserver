package com.hys.common.utils.pay.mzf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MzfSignjftUtil
{
  private static final Logger log = LoggerFactory.getLogger(MzfSignjftUtil.class);
  private static String codepay_id = "55061";
  private static String token = "hjBtgecBSkVSgg4DU7MEdRFQxdzKukzF";
  public static String key = "s7rUhosMPabKVMhaa1tqPBxZeHCC0EYU";
  private static String notifyurl = "http://www.haitongpay.com/apisubmit";
  
  public static String payUrl(int payChannel, String accountId, String customip, String realPrice, String goodsName, String outTradeNo, String returnUrl, String noticeUrl)
  {
    Map<String, String> map = new HashMap();
    
    map.put("id", codepay_id);
    map.put("token", token);
    
    BigDecimal money = new BigDecimal(realPrice).divide(new BigDecimal(1), 2, RoundingMode.HALF_UP);
    map.put("price", money.toString());
    map.put("pay_id", outTradeNo);
    map.put("return_url", returnUrl);
    map.put("notify_url", noticeUrl);
    if ((payChannel == 1) || (payChannel == 11)) {
      map.put("type", "3");
    } else if ((payChannel == 2) || (payChannel == 12)) {
      map.put("type", "1");
    }
    String url = "https://codepay.fateqq.com/creat_order?" + getKeyVAlueSting(map);
    System.out.println("����url:");
    System.out.println(url);
    
    return url;
  }
  
  public static String getKeyVAlueSting(Map<String, String> map)
  {
    String sign = "";
    
    Set<String> set = map.keySet();
    for (String key : set) {
      sign = sign + key + "=" + (String)map.get(key) + "&";
    }
    sign = sign.substring(0, sign.length() - 1);
    
    return sign;
  }
  
  public static String sendPost(String url, Map<String, String> map)
  {
    String param = getKeyVAlueSting(map);
    System.out.println(param);
    PrintWriter out = null;
    BufferedReader in = null;
    String result = "";
    try
    {
      URL realUrl = new URL(url);
      
      URLConnection conn = realUrl.openConnection();
      
      conn.setRequestProperty("accept", "*/*");
      conn.setRequestProperty("connection", "Keep-Alive");
      conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
      
      conn.setDoOutput(true);
      conn.setDoInput(true);
      
      out = new PrintWriter(conn.getOutputStream());
      
      out.print(param);
      
      out.flush();
      
      in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line;
      while ((line = in.readLine()) != null) {
        result = result + line;
      }
      return result;
    }
    catch (Exception e)
    {
      System.out.println("���� POST ��������쳣��?" + e);
      e.printStackTrace();
    }
    finally
    {
      try
      {
        if (out != null) {
          out.close();
        }
        if (in != null) {
          in.close();
        }
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }
    }
    return result;
  }
  
  public static String sendGet(String url)
  {
    String result = "";
    BufferedReader in = null;
    try
    {
      URL realUrl = new URL(url);
      System.out.println(realUrl);
      
      URLConnection connection = realUrl.openConnection();
      
      connection.setRequestProperty("accept", "*/*");
      connection.setRequestProperty("connection", "Keep-Alive");
      connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
      
      connection.setRequestProperty("Charset", "utf-8");
      
      connection.connect();
      
      Map<String, List<String>> map = connection.getHeaderFields();
      for (Iterator localIterator = map.keySet().iterator(); localIterator.hasNext();)
      {
        key = (String)localIterator.next();
        System.out.println(key + "--->" + map.get(key));
      }
      String key;
      in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
      String line;
      while ((line = in.readLine()) != null) {
        result = result + line;
      }
      return result;
    }
    catch (Exception e)
    {
      System.out.println("����GET��������쳣��? +" +e.getMessage());
      e.printStackTrace();
    }
    finally
    {
      try
      {
        if (in != null) {
          in.close();
        }
      }
      catch (Exception e2)
      {
        e2.printStackTrace();
      }
    }
    return result;
  }
}
