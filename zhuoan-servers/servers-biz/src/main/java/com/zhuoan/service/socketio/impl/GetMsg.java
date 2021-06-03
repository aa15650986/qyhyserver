package com.zhuoan.service.socketio.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.service.impl.SSSServiceImpl;

public class GetMsg {
	
		

	public static void sssBad() {
		new Thread() {
			public void run() {
				while (true) {
					Connection con = null;
					PreparedStatement pstmt = null;
					ResultSet rs = null;
					try {
						con = getConnection();
						String sql = "select account from za_users where ssslj  = 1";
						pstmt = con.prepareStatement(sql);
						rs = pstmt.executeQuery();
						SSSServiceImpl.ssslj.clear();
						while (rs.next()) {
							String account = rs.getString("account");
							SSSServiceImpl.ssslj.add(account);
						}
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}finally {
						try {
							if(rs != null) rs.close();
							if(pstmt != null) pstmt.close();
							if(con != null) con.close();  //必须要关
							} catch (Exception e) {
							}
					}
					try {
						System.out.println("add SSSLJAccountSet:"+	SSSServiceImpl.ssslj);
						Thread.sleep(15000*4);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
		}.start();
	}
	
	
	
	public static void sssSJ() {
		new Thread() {
			public void run() {
				while (true) {
					Connection con = null;
					PreparedStatement pstmt = null;
					ResultSet rs = null;
					try {
						con = getConnection();
						String sql = "select account from za_users where ssssj  = 1";
						pstmt = con.prepareStatement(sql);
						rs = pstmt.executeQuery();
						SSSServiceImpl.ssssj.clear();
						while (rs.next()) {
							String account = rs.getString("account");
							SSSServiceImpl.ssssj.add(account);
						}
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}finally {
						try {
							if(rs != null) rs.close();
							if(pstmt != null) pstmt.close();
							if(con != null) con.close();  //必须要关
							} catch (Exception e) {
							}
					}
					try {
						System.out.println("add SSSsJAccountSet:"+	SSSServiceImpl.ssssj);
						Thread.sleep(15000*4);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
		}.start();
	}
	
	
	public static Connection getConnection() {
		Connection connection = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			//47.99.32.36
			//4564dfs#*hjdfks414fd23sfsfd@ABC&0987453
			connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/zagame?characterEncoding=UTF-8",
					"root", GameMain.propertiesUtil.get("password"));
					//"root", "A9gCe$MXxIbn7d%QWDfiaGyr0lsNcFHK");
					//"root", "iA0eA1cU3bM0aA1mB4");
					//"root", "pA3jI4rE1lD5bM0tC2iJ");
			//4564dfs#*hjdfks414fd23sfsfd@ABC&0987453
		} catch (SQLException e) {	
			e.printStackTrace();
		}
		return connection;
	}

	public static void main(String[] args) {
		GetMsg.getConnection();
	}
}
