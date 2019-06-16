package io.binakot.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Random;

public class DemoApplication {

    private static final int OBJECT_COUNT = 10;
    private static final int TELEMETRY_COUNT_PER_OBJECT = 1_000;

    private static final String INSERT_OBJECT_QUERY =
        "INSERT INTO objects (name) " +
        "VALUES (?)";
    private static final String INSERT_TELEMETRY_QUERY =
        "INSERT INTO telemetry_stream (object_id, time, latitude, longitude, course, speed) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    public static void main(final String... args) throws Exception {
        final String host = System.getProperty("PG_HOST", "localhost");
        try (final Connection conn = DriverManager.getConnection("jdbc:postgresql://" + host + ":5432/postgres", "postgres", "postgres")) {
            // Check extensions
            try (final Statement stmt = conn.createStatement();
                 final ResultSet rs = stmt.executeQuery("SELECT * FROM pg_extension")) {
                System.out.printf("%-15.15s %-15.15s%n", "Name", "Version");
                while (rs.next()) {
                    System.out.printf("%-15.15s %-15.15s%n", rs.getString("extname"), rs.getString("extversion"));
                }
                System.out.println();
            }
            // Generate objects
            try (final PreparedStatement stmt = conn.prepareStatement(INSERT_OBJECT_QUERY)) {
                for (int i = 0; i < OBJECT_COUNT; i++) {
                    final String objectName = "Object" + i;
                    stmt.setString(1, objectName);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            // Generate telemetry data
            try (final PreparedStatement stmt = conn.prepareStatement(INSERT_TELEMETRY_QUERY)) {
                final Random rand = new Random(System.currentTimeMillis());
                for (int i = 0; i < OBJECT_COUNT * TELEMETRY_COUNT_PER_OBJECT; i++) {
                    final int objectId = i % OBJECT_COUNT + 1;
                    stmt.setInt(1, objectId);
                    stmt.setTimestamp(2, Timestamp.from(Instant.now()));
                    stmt.setDouble(3, (rand.nextDouble() * -180.0) + 90.0);
                    stmt.setDouble(4, (rand.nextDouble() * -360.0) + 180.0);
                    stmt.setFloat(5, rand.nextFloat() * 360f);
                    stmt.setFloat(6, rand.nextFloat() * 120f);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            // Check PipelineDB's view with stats
            try (final Statement stmt = conn.createStatement()) {
                try (final ResultSet rs = stmt.executeQuery("SELECT * FROM object_states ORDER BY object_name")) {
                    System.out.printf("%-10.10s   %-10.10s   %-10.10s%n", "Name", "Avg Speed", "P95 Speed");
                    while (rs.next()) {
                        final String objectName = rs.getString("object_name");
                        final double avgSpeed = rs.getDouble("avg_speed");
                        final double p95Speed = rs.getDouble("p95_speed");
                        System.out.printf("%-10.10s   %-10.10s   %-10.10s%n", objectName, avgSpeed, p95Speed);
                    }
                    System.out.println();
                }
            }
            // Check TimescaleDB's hyper-table with stored telemetries
            try (final Statement stmt = conn.createStatement()) {
                try (final ResultSet rs = stmt.executeQuery("SELECT DISTINCT ON (object_id) * FROM telemetries ORDER BY object_id, time DESC")) {
                    System.out.printf("%-10.10s   %-25.25s   %-10.10s   %-10.10s   %-10.10s%n", "ID", "Time", "Lat", "Lng", "Course");
                    while (rs.next()) {
                        final int objectId = rs.getInt("object_id");
                        final Timestamp time = rs.getTimestamp("time");
                        final double lat = rs.getDouble("latitude");
                        final double lng = rs.getDouble("longitude");
                        final float course = rs.getFloat("course");
                        System.out.printf("%-10.10s   %-25.25s   %-10.10s   %-10.10s   %-10.10s%n", objectId, time, lat, lng, course);
                    }
                    System.out.println();
                }
            }
        }
    }
}
