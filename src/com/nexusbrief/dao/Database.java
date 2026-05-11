package com.nexusbrief.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String URL = "jdbc:sqlserver://DESKTOP-K5N8DRE\\SQLEXPRESS;databaseName=NexusBrief;integratedSecurity=true;encrypt=true;trustServerCertificate=true;loginTimeout=10;";
    private static final String USER = "";
    private static final String PASS = "";

    private static Connection conn;

    // Sets up the data base connection
    public static void init() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("[DB] Connected successfully");
        } catch (Exception e) {
            System.err.println("[DB] Connection failed: " + e.getMessage());
        }
    }

    public static Connection getConn() {
        try {
            if (conn == null || conn.isClosed())
                init();
        } catch (SQLException e) {
            init();
        }
        return conn;
    }

    public static void close() {
        try {
            if (conn != null && !conn.isClosed())
                conn.close();
        } catch (SQLException ignored) {
        }
    }

    public static void initAdminUser() {
        try {
            Connection c = getConn();
            try (PreparedStatement check = c.prepareStatement("SELECT COUNT(*) FROM users WHERE isAdmin=1")) {
                ResultSet rs = check.executeQuery();
                if (rs.next() && rs.getInt(1) > 0)
                    return;
            }
            String id = java.util.UUID.randomUUID().toString();

            // setup admin user name and idpassword in database
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users(userID,name,email,passwordHash,isVerified,createdAt,isAdmin) VALUES(?,?,?,?,?,?,?)")) {
                ps.setString(1, id);
                ps.setString(2, "Admin");
                ps.setString(3, "admin@nexusbrief.com");
                ps.setString(4, "Admin123");
                ps.setBoolean(5, true); // for verification that he is admin = 1
                ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
                ps.setBoolean(7, true);
                ps.executeUpdate();
            }
            System.out.println("[DB] Admin created → admin@nexusbrief.com / Admin123");
        } catch (Exception e) {
            System.err.println("[DB] initAdminUser: " + e.getMessage());
        }
    }
}

// for mapping the resultset to an object
interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
}

// BASE DATA BASE OBJECT CLASS
abstract class BaseDAO {

    protected Connection conn() {
        return Database.getConn();
    }

    // execute update,INSERT,delete opterations
    protected boolean exec(String sql, Object... params) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO] exec failed: " + e.getMessage());
            return false;
        }
    }

    // it will fetch exactly one row then it will convert it to aa java obj [ FOR
    // ONE QUERY]
    protected <T> T queryOne(String sql, RowMapper<T> m, Object... params) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bind(ps, params);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? m.map(rs) : null;
        } catch (SQLException e) {
            System.err.println("[DAO] queryOne failed: " + e.getMessage());
            return null;
        }
    }

    // it will fetch multiple rows then it will convert them into java obj in form
    // of list [ FOR MULTIPLE QUERIES]
    protected <T> List<T> queryMany(String sql, RowMapper<T> m, Object... params) {
        List<T> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bind(ps, params);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(m.map(rs));
        } catch (SQLException e) {
            System.err.println("[DAO] queryMany failed: " + e.getMessage());
        }
        return list; // Rtrns query <LIST>
    }

    // HERE WE WILL INSERT java val into ???? place holdors seql
    private void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null)
                ps.setNull(i + 1, Types.NULL);
            else if (p instanceof String)
                ps.setString(i + 1, (String) p);
            else if (p instanceof Boolean)
                ps.setBoolean(i + 1, (Boolean) p);
            else if (p instanceof Integer)
                ps.setInt(i + 1, (Integer) p);
            else if (p instanceof Float)
                ps.setFloat(i + 1, (Float) p);
            else if (p instanceof LocalDateTime)
                ps.setTimestamp(i + 1, Timestamp.valueOf((LocalDateTime) p));
            else if (p instanceof LocalTime)
                ps.setTime(i + 1, Time.valueOf((LocalTime) p));
            else
                ps.setObject(i + 1, p);
        }
    }

    protected LocalDateTime toDate(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    protected Timestamp toTS(LocalDateTime dt) {
        return dt == null ? null : Timestamp.valueOf(dt);
    }
}
