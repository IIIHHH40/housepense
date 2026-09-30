package org.example;
import org.json.JSONObject;
public class JdbcUtil {
    //Secrets managerからとってきたJSON文字列をJavaの形に変換する
    public static DbConfig parseSecret(String secretJson){
        JSONObject json=new JSONObject(secretJson);
        String host=json.getString("host");
        String dbname=json.getString("dbname");
        String username=json.getString("username");
        String password=json.getString("password");
        int port=json.getInt("port");
        return new DbConfig(host,port,dbname,username,password);
    }
    //JavaオブジェクトからJDBC URLを生成
    public static String createJdbcUrl(DbConfig config){
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                config.host(),
                config.port(),
                config.dbname()
        );
    }

}