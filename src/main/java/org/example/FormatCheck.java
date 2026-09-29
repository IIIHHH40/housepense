package org.example;
public class FormatCheck{
    public static FormatStatus check(String text){
        if (text==null || text.isBlank()){
            return FormatStatus.isvalid("入力が空です。例:1200 食費 めも");
        }
        String[] parts=text.trim().split("\\s+");

        //金額とカテゴリは必須
        if (parts.length<2){
            return FormatStatus.isvalid("フォーマットが正しくありません。例:1200 食費 めも");
        }
        //数値チェック
        try{
            Integer.parseInt(parts[0]);
        }catch (NumberFormatException e){
            return FormatStatus.isvalid("金額は数値で入力してください。例:1200 食費 めも");
        }
        return FormatStatus.valid();
    }
}
