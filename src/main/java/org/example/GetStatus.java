package org.example;
import java.sql.*;
import java.util.*;

public class GetStatus {
    public static List<DailyTotal> loadDailyTotals(String jdbcUrl,String user,String pass,String userId){
        List<DailyTotal> list=new ArrayList<>();
        String sql="""
                SELECT DATE(CONVERT_TZ(created_at,'+00:00'+'09:00')) As day, SUM(amount) AS total
                FROM expenses
                WHERE user_id=?
                GROUP BY day
                ORDER BY day
                """;
        try(Connection conn=DriverManager.getConnection(jdbcUrl,user,pass);
            PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setString(1,userId);
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()){
                    list.add(new DailyTotal(
                            rs.getString("day"),
                            rs.getInt("total")
                    ));
                }
            }
        }catch (SQLException e){
            throw new RuntimeException("Load daily totals failed", e);
        }
        return list;
    }
}