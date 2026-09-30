package org.example;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;


public class Handler implements RequestHandler<Map<String,Object>,String>{
    @Override
    public String handleRequest(Map<String, Object>  input, Context context){
        try{
            context.getLogger().log("Input:"+input);
            String body=(String)input.get("body");
            if(body==null){
                context.getLogger().log("boddy is null");
                return "OK";
            }
            JSONObject json=new JSONObjet(body);
            JSONArray events=json.optJSONArray("events");
            if(events==null || events.length()==0){
                //Webhook検証の時
                context.getLogger().log("no events");
                return "OK";

            }
            JSONObject event=events.getJSONObject(0);
            JSONObject source=event.getJSONObject("source");
            String userId=source.getString("userId");
            String text=message.getString("text");
            context.getLogger().log("userId:"+userId);
            context.getLogger().log("text:"+text);
            if("グラフ".equals(text)){
                String secretJson=SecretUtil.getSecret("household-secret");
                DbCOnfig config=JdbcUtil.parseSecret(secretJson);
                String jdbcUrl=JdbcUtil.createJdbcUrl(config);
                List<DailyTotal> totals=GetStatus.loadDailyTotals(
                        jdbcUrl,
                        config.username(),
                        config.password(),
                        userId
                );
                String graphText=buildDailyTotalGraph(totals);
                replyToke(replyToken,graphText,context);
                return "Ok";
            }
            FormatStatus status=FormatCheck.check(text);
            if(!status.ok()){
                replyToken(replyToken,status.message(),context);
                return "OK";
            }

            else{
                String[] parts=FormatCheck.split(text);
                int amount=Integer.parseInt(parts[0]);
                String category=parts[1];
                String memo=(parts.length>=3) ? parts[2]:"";
                //Secrets->JDBC->INSERT
                String secretJson=SecretsUtil.getSecret("household-secret");
                DbConfig config=JdbcUtil.parseSecret(secretJson);
                String jdbcUrl=JdbcUtil.createJdbcUrl(config);
                DbAccess.insertRecord(
                        jdbcUrl,
                        config.username(),
                        config.password(),
                        userId,
                        amount,
                        category,
                        memo);}
            return "OK";
        } catch(Exception e){
            throw e;
        }
    }
    private String buildDailyGraphText(java.util.List<DailyTotal> totals){
        if (totals.isEmpty()){
            return "まだ記録がないです";
        }

        StringBuilder sb=new StringBuilder();
        sb.append("日ごとの合計金額\n\n");
        int max=0;
        for(DayiyTotal dt:totals){
            if (dt.total()>max) max=dt.tatal();
        }
        double unit=max/20.0;
        for(DailyTotal dt:totals){
            String day=dt.day();
            int total=dt.total();
            int barLen=unit>0 ? (int)Math.roud(total/unit):0;
            if(barLen==0 && total>0) barLen=1;
            String bar="#".repeat(Math.max(barLen,0));
            sb.append(day)
                    .append(" : ")
                    .append(bar)
                    .append("  ")
                    .append(total)
                    .append("円\n");
        }
        return sb.toString();
    }
    private void replyToken(String replyToken,String message,Context context){
        try{
            String channelAccessToken=System.getenv("LINE_CHANNEL_ACCESS_TOKEN");
            java.net.URL url=new java.net.URL("https://api.line.me/v2/bot/message/reply");
            java.net.HttpURLConnection conn=(java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            conn.setRequestProperty("Authorization","Bearer "+channelAccessToken);
            org.json.JSONObject json=new org.json.JSONObject();
            org.json.JSONObject msgObj=new org.json.JSONObject();
            msgObj.put("type","text");
            msgObj.put("text",message);
            message.put(msgObj);
            body.put("message",message);

            byte[] out=body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try(java.io.OutputStrem os=conn.getOutputStrem()){
                os.write(out);
            }
            int status=conn.getResponseCode();
            context.getLogger().log("LINE reply status:"+status);
        }catch(Exception e){
            context.getLogger().log("LINE reply error: "+e);
        }
    }
}

