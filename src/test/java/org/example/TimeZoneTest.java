package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 「DB には UTC で保存し、集計時に JST の日付に変換する」ことの回帰テスト。
 *
 * 過去のバグ: DATE(created_at) で UTC の日付をそのまま使っていたため、
 * 日本時間 0:00〜8:59 の支出が前日に集計されていた。
 */
@Testcontainers
class TimeZoneTest {

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    @Container
    static MySQLContainer<?> utcDb = new MySQLContainer<>("mysql:8.0")
            .withInitScript("schema.sql");

    @Container
    static MySQLContainer<?> jstDb = new MySQLContainer<>("mysql:8.0")
            .withInitScript("schema.sql")
            .withCommand("--default-time-zone=+09:00");

    @BeforeEach
    void clean() throws SQLException {
        truncate(utcDb);
        truncate(jstDb);
    }

    // ===== 日別集計(GetStatus.loadDailyTotals) =====

    @Test
    void UTCで15時をまたぐとJSTでは日付が変わる() throws SQLException {
        insertAt(utcDb, "u1", 500,  "2026-09-28 14:59:59"); // JST 9/28 23:59:59
        insertAt(utcDb, "u1", 1000, "2026-09-28 15:00:00"); // JST 9/29 00:00:00

        List<DailyTotal> totals = load(utcDb, "u1");

        assertEquals(List.of(
                new DailyTotal("2026-09-28", 500),
                new DailyTotal("2026-09-29", 1000)
        ), totals);
    }

    @Test
    void UTCでは別の日でもJSTで同じ日なら合算される() throws SQLException {
        insertAt(utcDb, "u1", 300, "2026-09-28 15:30:00"); // JST 9/29 00:30
        insertAt(utcDb, "u1", 700, "2026-09-29 14:30:00"); // JST 9/29 23:30

        List<DailyTotal> totals = load(utcDb, "u1");

        assertEquals(List.of(new DailyTotal("2026-09-29", 1000)), totals);
    }

    @Test
    void 他のユーザーの支出は含まれない() throws SQLException {
        insertAt(utcDb, "u1", 1000, "2026-09-28 03:00:00");
        insertAt(utcDb, "u2", 9999, "2026-09-28 03:00:00");

        List<DailyTotal> totals = load(utcDb, "u1");

        assertEquals(List.of(new DailyTotal("2026-09-28", 1000)), totals);
    }

    // ===== 保存(DbAccess.insertRecord) =====

    @Test
    void DBのtime_zoneがUTCでもUTCで保存される() throws SQLException {
        DbAccess.insertRecord(utcDb.getJdbcUrl(), utcDb.getUsername(), utcDb.getPassword(),
                "u1", 1200, "食費", "ランチ");

        assertStoredInUtc(utcDb);
    }

    @Test
    void DBのtime_zoneがJSTに設定されていてもUTCで保存される() throws SQLException {
        // 前提: このDBは本当にJST設定になっている(NOW() と UTC_TIMESTAMP() が9時間ずれる)
        try (Connection c = connect(jstDb); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT TIMESTAMPDIFF(HOUR, UTC_TIMESTAMP(), NOW())")) {
            rs.next();
            assertEquals(9, rs.getInt(1), "テスト用DBがJST設定になっていない");
        }

        DbAccess.insertRecord(jstDb.getJdbcUrl(), jstDb.getUsername(), jstDb.getPassword(),
                "u1", 1200, "食費", "ランチ");

        assertStoredInUtc(jstDb);
    }

    @Test
    void 保存してから集計すると今日のJSTの日付になる_DBがJST設定でも二重にずれない() {
        for (MySQLContainer<?> db : List.of(utcDb, jstDb)) {
            LocalDate before = LocalDate.now(JST);
            DbAccess.insertRecord(db.getJdbcUrl(), db.getUsername(), db.getPassword(),
                    "u1", 1200, "食費", "ランチ");
            LocalDate after = LocalDate.now(JST);

            List<DailyTotal> totals = load(db, "u1");

            assertEquals(1, totals.size());
            String day = totals.get(0).day();
            // 日付が変わる瞬間に実行されても落ちないよう、前後どちらかと一致すればOK
            assertTrue(day.equals(before.toString()) || day.equals(after.toString()),
                    "JSTの今日の日付ではない: " + day);
            assertEquals(1200, totals.get(0).total());
        }
    }


    private static Connection connect(MySQLContainer<?> db) throws SQLException {
        return DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
    }

    private static void truncate(MySQLContainer<?> db) throws SQLException {
        try (Connection c = connect(db); Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE expenses");
        }
    }

    private static List<DailyTotal> load(MySQLContainer<?> db, String userId) {
        return GetStatus.loadDailyTotals(db.getJdbcUrl(), db.getUsername(), db.getPassword(), userId);
    }

    private static void insertAt(MySQLContainer<?> db, String userId, int amount, String createdAtUtc)
            throws SQLException {
        try (Connection c = connect(db);
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO expenses(user_id, amount, category, memo, created_at) VALUES (?, ?, '食費', NULL, ?)")) {
            ps.setString(1, userId);
            ps.setInt(2, amount);
            ps.setString(3, createdAtUtc);
            ps.executeUpdate();
        }
    }

    private static void assertStoredInUtc(MySQLContainer<?> db) throws SQLException {
        try (Connection c = connect(db); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT TIMESTAMPDIFF(SECOND, created_at, UTC_TIMESTAMP()) FROM expenses")) {
            assertTrue(rs.next(), "保存されていない");
            long diff = rs.getLong(1);
            assertTrue(0 <= diff && diff < 60, "UTCで保存されていない(差: " + diff + "秒)");
        }
    }
}