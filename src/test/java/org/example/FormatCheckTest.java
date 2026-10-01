package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatCheckTest {


    @Test
    void 正しい形式は通る() {
        FormatStatus status = FormatCheck.check("1200 食費 お寿司");

        assertTrue(status.isValid());
    }

    @Test
    void メモがなくても通る() {
        FormatStatus status = FormatCheck.check("1200 食費");

        assertTrue(status.isValid());
    }

    @Test
    void 全角スペース区切りでも通る() {
        FormatStatus status = FormatCheck.check("1200　食費　お寿司");

        assertTrue(status.isValid());
    }
    //弾く

    @Test
    void 空文字は弾く() {
        FormatStatus status = FormatCheck.check("");

        assertFalse(status.isValid());
        assertTrue(status.message().startsWith("入力が空です"));
    }

    @Test
    void nullは弾く() {
        FormatStatus status = FormatCheck.check(null);

        assertFalse(status.isValid());
        assertTrue(status.message().startsWith("入力が空です"));
    }

    @Test
    void カテゴリがなければ弾く() {
        FormatStatus status = FormatCheck.check("1200");

        assertFalse(status.isValid());
        assertTrue(status.message().startsWith("フォーマットが正しくありません"));
    }

    @Test
    void 金額が数字でなければ弾く() {
        FormatStatus status = FormatCheck.check("千円 食費");

        assertFalse(status.isValid());
        assertTrue(status.message().startsWith("金額は数値で入力してください"));
    }

    @Test
    void マイナスの金額は弾く() {
        FormatStatus status = FormatCheck.check("-500 食費");

        assertFalse(status.isValid());
    }

    //splitに関するテスト

    @Test
    void 半角スペースで3つに分けられる() {
        String[] parts = FormatCheck.split("1200 食費 お寿司");

        assertArrayEquals(new String[]{"1200", "食費", "お寿司"}, parts);
    }

    @Test
    void 全角スペースでも3つに分けられる() {
        String[] parts = FormatCheck.split("1200　食費　お寿司");

        assertArrayEquals(new String[]{"1200", "食費", "お寿司"}, parts);
    }

    @Test
    void スペースが2つ続いても空の要素ができない() {
        // 以前の split(" ") では ["1200", "", "食費", "お寿司"] になり、カテゴリが空で保存されていた
        String[] parts = FormatCheck.split("1200  食費 お寿司");

        assertArrayEquals(new String[]{"1200", "食費", "お寿司"}, parts);
    }

}