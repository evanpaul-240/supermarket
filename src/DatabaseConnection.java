import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Loads local connection settings and opens JDBC connections to MySQL. */
public final class DatabaseConnection {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/supermarket_billing?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private final String url;
    private final String username;
    private final String password;

    private DatabaseConnection(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public static DatabaseConnection load() throws IOException {
        Properties properties = new Properties();
        Path configPath = Path.of("config", "db.properties");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }

        return new DatabaseConnection(
                setting("DB_URL", properties, "db.url", DEFAULT_URL),
                setting("DB_USER", properties, "db.user", "root"),
                setting("DB_PASSWORD", properties, "db.password", ""));
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(String environmentName, Properties properties,
                                  String propertyName, String fallback) {
        String fromEnvironment = System.getenv(environmentName);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) return fromEnvironment;
        String fromFile = properties.getProperty(propertyName);
        return fromFile == null ? fallback : fromFile.trim();
    }
}
