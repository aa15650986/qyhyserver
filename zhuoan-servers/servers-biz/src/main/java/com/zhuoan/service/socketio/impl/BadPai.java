package com.zhuoan.service.socketio.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.service.impl.PDKServiceImpl;

public class BadPai {

	
	
	public static void creatServer() throws IOException {
		HttpServer httpServer = HttpServer.create(new InetSocketAddress(8001), 0);
		httpServer.createContext("/setBadPdk", new TestHandler());
		httpServer.start();
		System.out.println("8001端口已成功启动");
	}
	
	static class TestHandler implements HttpHandler {
		@Resource
		private RobotEventDeal robotEventDeal;
		
		public Map<String, String> queryToMap(String query) {
			Map<String, String> result = new HashMap<String, String>();
			for (String param : query.split("&")) {
				String pair[] = param.split("=");
				if (pair.length > 1) {
					result.put(pair[0], pair[1]);
				} else {
					result.put(pair[0], "");
				}
			}
			return result;
		}

		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			// 返回
			String rtn = "调用成功";
			httpExchange.sendResponseHeaders(200, 0);
			// 获取请求路径
			URI requestURI = httpExchange.getRequestURI();
			System.out.println("请求路径为：" + requestURI);
			// 获取请求方法
			String requestMethod = httpExchange.getRequestMethod();
			System.out.println("请求方法为：" + requestMethod);
			// 获取请求体
			Map<String, String> map2 = queryToMap(requestURI.getQuery());
			System.out.println(map2);
			String eid = map2.get("eid");
			OutputStream responseBody = httpExchange.getResponseBody();
			switch (eid) {
			case "1":
				PDKServiceImpl.accountSet.add(map2.get("account"));
				System.out.println("盟主号添加成功");
				responseBody.write("盟主号添加成功".getBytes());
				break;
			case "2":
				PDKServiceImpl.accountSetlaji.add(map2.get("account"));
				System.out.println("跑得快垃圾牌添加成功");
				responseBody.write("跑得快垃圾牌添加成功".getBytes());
				break;
			case "3":
				PDKServiceImpl.accountSet.remove(map2.get("account"));
				System.out.println("移除跑得快盟主号成功");
				responseBody.write("移除跑得快盟主号成功".getBytes());
				break;
			case "4":
				PDKServiceImpl.accountSetlaji.remove(map2.get("account"));
				System.out.println("移除跑得快垃圾号成功");
				responseBody.write("跑得快垃圾号移除成功".getBytes());
				break;
			case "5":
				String str = PDKServiceImpl.accountSet.stream().map(integer -> integer.toString()).collect(Collectors.joining(","));
				responseBody.write(("盟主号："+str).getBytes());
				break;
			case "6":
				String str2 = PDKServiceImpl.accountSetlaji.stream().map(integer -> integer.toString()).collect(Collectors.joining(","));
				responseBody.write(("垃圾牌号："+str2).getBytes());
				break;
			case "7":
				PDKServiceImpl.accountSet.clear();
				responseBody.write("盟主号集合清除成功".getBytes());
				break;
			case "8":
				PDKServiceImpl.accountSetlaji.clear();
				responseBody.write("垃圾牌用户清除成功".getBytes());
				break;
				
			case "9":
				System.out.println("设置房间为人机房");
				String roomNo = map2.get("roomNo");
				System.out.println(roomNo);
				RobotEventDeal.roomNoList.add(roomNo);
				RoomManage.gameRoomMap.get(roomNo).setRobotRoom(true);
			default:
				break;
			}
			responseBody.close();
			/*
			 * InputStream requestBody = httpExchange.getRequestBody(); InputStreamReader
			 * inputStreamReader = new InputStreamReader(requestBody); BufferedReader
			 * bufferedReader = new BufferedReader(inputStreamReader); StringBuffer
			 * stringBuffer = new StringBuffer(); String s = ""; while ((s =
			 * bufferedReader.readLine()) != null) { stringBuffer.append(s.trim()); }
			 * 
			 * System.out.println(stringBuffer); // 此处引入谷歌Gson框架将String转为Map方便获取参数 Gson gson
			 * = new Gson(); Map map = gson.fromJson(stringBuffer.toString(), new
			 * TypeToken<Map<String, Object>>() { }.getType()); System.out.println("请求参数为:"
			 * + map.get("account")); System.out.println("请求参数为:" + map.get("phone"));
			 */
			
		}
	}

}
