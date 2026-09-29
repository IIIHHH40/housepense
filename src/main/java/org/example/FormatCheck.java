package org.example;
public class FormatCheck{
    private static final int MAX_AMOUNT=10_000_000;

    public static FormatStatus check(String text){

        if (text==null || text.isBlank()){
            return FormatStatus.invalid("入力が空です。例:1200 食費 めも");
        }
        String[] parts=text.split("(?U)\\s+");
        //金額とカテゴリは必須
        if (parts.length<2){
            return FormatStatus.invalid("フォーマットが正しくありません。例:1200 食費 めも");
        }
        //数値チェック
        int amount;
        try{
            amount=Integer.parseInt(parts[0]);
        }catch (NumberFormatException e){
            return FormatStatus.invalid("金額は数値で入力してください。例:1200 食費 めも");
        }
        if (amount<0 || amount>MAX_AMOUNT ){
            return FormatStatus.invalid("金額は0~10000000の範囲で入力してください");
        }
        return FormatStatus.valid();
    }
}
