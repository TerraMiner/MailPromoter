package ua.terra;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseConnection {

    private Connection connection;

    public DatabaseConnection(String host, String port, String user, String password, String base) throws SQLException {
        String address = "jdbc:mysql://%s:%s/%s".formatted(host, port, base);
        connection = DriverManager.getConnection(address, user, password);
    }

    public void close() throws SQLException {
        connection.close();
    }

    public boolean isClosed() throws SQLException {
        return connection.isClosed();
    }

    public List<String> getMails() {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM mails;")) {

            List<String> list = new ArrayList<>();

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String value = resultSet.getString("value");
                    list.add(value);
                }
            }

            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
