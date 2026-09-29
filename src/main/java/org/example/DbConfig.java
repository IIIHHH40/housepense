package org.example;
public class DbConfig{
    private final String host;
    private final String dbname;
    private final String username;
    private final String password;
    private final int port;
    public DbConfig(String host,int port,String dbname,String username,String password){
        this.host=host;
        this.dbname=dbname;
        this.username=username;
        this.password=password;
        this.port=port;
    }
    public String host(){return host;}
    public String dbname(){return dbname;}
    public String username(){return username;}
    public String password(){return password;}
    public int port() {return port;}

}
