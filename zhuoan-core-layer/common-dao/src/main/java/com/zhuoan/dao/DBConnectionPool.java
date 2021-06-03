package com.zhuoan.dao;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DBConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(DBConnectionPool.class);
    private static DruidDataSource dataSource;

    @Autowired
    public void setDruidDataSource(DruidDataSource dataSource) {
        DBConnectionPool.dataSource = dataSource;
    }

    private DBConnectionPool() {
    }

    public static final Connection getConnection() {
        DruidPooledConnection conn = null;

        try {
            conn = dataSource.getConnection();
        } catch (SQLException var2) {
            logger.error("", var2);
        }

        return conn;
    }

    public static void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }

        } catch (SQLException var2) {
            throw new RuntimeException("关闭数据库连接失败");
        }
    }
}
