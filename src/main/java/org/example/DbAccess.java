package org.example;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
public class DbAccess{
    public static void  insertRecord(String jdbcUrl,String user,String pass,String userId,int amount, String category,String memo){
        try(Connection conn=DriverManager.getConnection(jdbcUrl,user,pass)){
            String sql="INSERT INTO expenses(user_id,amount,category,memo,created_at) VALUES(?,?,?,?,UTC_TIMESTAMP())";
            PreparedStatement ps=conn.prepareStatement(sql);
            ps.setString(1,userId);
            ps.setInt(2,amount);
            ps.setString(3,category);
            ps.setString(4,memo);
            ps.executeUpdate();

            System.out.println("Insert Success");
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException("Insert Failed",e);
        }
    }
}