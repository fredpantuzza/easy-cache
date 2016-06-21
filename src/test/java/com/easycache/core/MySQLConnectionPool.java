package com.easycache.core;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class MySQLConnectionPool implements Closeable {

    private final Connection[] connections = new Connection[10];

    private final Set<Connection> freeConnections = new HashSet<Connection>();

    public MySQLConnectionPool() throws ClassNotFoundException, SQLException {
        String driverName = "com.mysql.jdbc.Driver";
        Class.forName(driverName);

        String serverName = "localhost";
        String mydatabase = "blog_cache";
        String url = "jdbc:mysql://" + serverName + "/" + mydatabase;
        String username = "root";
        String password = "";

        for (int i = 0; i < 10; i++) {
            Connection connection = DriverManager.getConnection(url, username, password);
            this.connections[i] = connection;
            this.freeConnections.add(connection);
        }
    }

    public Connection getConnection() {
        synchronized (this) {
            Connection connection = this.freeConnections.iterator().next();
            this.freeConnections.remove(connection);

            return connection;
        }
    }

    public void releaseConnection(Connection connection) {
        synchronized (this) {
            this.freeConnections.add(connection);
        }
    }

    @Override
    public void close() throws IOException {
        for (Connection connection : this.connections) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
