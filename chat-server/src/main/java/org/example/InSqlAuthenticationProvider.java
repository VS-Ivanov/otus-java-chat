package org.example;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class InSqlAuthenticationProvider implements AuthenticationProvider {

    private static final String DB_URL = "jdbc:sqlite::resource:auth.db";
    private static final String GET_USER_QUERY = "select username from Users where login = '%s' and password = '%s';";

    private static final String REGISTER_USER_QUERY = "insert into Users (login,username,password,role) values ('%s','%s','%s','%s');";

    private static final String GET_USER_ROLE_QUERY = "select role from Users where username = '%s'";

    private Connection dbConn;
    public InSqlAuthenticationProvider() {

        try{
            this.dbConn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {

        try(Statement stmt = dbConn.createStatement()) {

            String passHash = md5hash(password);
            if(passHash == null) {return null;}

            //System.out.println(String.format(GET_USER_QUERY,login,password));
            ResultSet rs = stmt.executeQuery(String.format(GET_USER_QUERY,login,passHash));

            if(rs.next()) {
                return rs.getString("username");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean register(String login, String password, String username, UserRole role) {

        try(Statement stmt = dbConn.createStatement()) {

            String passHash = md5hash(password);
            if(passHash == null) {return false;}

            //System.out.println(String.format(REGISTER_USER_QUERY,login,username,passHash,role));
            stmt.execute(String.format(REGISTER_USER_QUERY,login,username,passHash,role));

            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public UserRole getUserRoleByUsername(String username) {

        try(Statement stmt = dbConn.createStatement()) {

            //System.out.println(String.format(GET_USER_ROLE_QUERY,username));
            ResultSet rs = stmt.executeQuery(String.format(GET_USER_ROLE_QUERY,username));

            if(rs.next()) {

                UserRole result = null;
                String roleName = rs.getString("role");

                for(UserRole role: UserRole.values()) {
                    if(role.name().equalsIgnoreCase(roleName)) {
                        result = role;
                        break;
                    }
                }
                return result;

            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //получаем хеш пароля для безопасного хранения
    private static String md5hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte[] hash = md.digest();
            return String.format("%X", ByteBuffer.wrap(hash).getLong());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void finalize() {

        if(dbConn != null) {
            try{
                dbConn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }


}
