package wat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.sql.DataSource;

public class DataSourceProvider {

    private static HikariConfig config = loadHikariConfig();
    private static HikariDataSource dataSource = new HikariDataSource(config);

    public static DataSource getDataSource() {
        if (dataSource.isClosed()) {
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (
            InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("dbconfig.properties")
        ) {
            properties.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    private static HikariConfig loadHikariConfig() {
        if (config == null) {
            Properties properties = loadProperties();
            config = new HikariConfig();
            config.setJdbcUrl(properties.getProperty("jdbcUrl"));
            config.setUsername(properties.getProperty("username"));
            config.setPassword(properties.getProperty("password"));
            config.addDataSourceProperty(
                "cachePrepStmts",
                properties.getProperty("cachePrepStmts")
            );
            config.addDataSourceProperty(
                "prepStmtCacheSize",
                properties.getProperty("prepStmtCacheSize")
            );
            config.addDataSourceProperty(
                "prepStmtCacheSqlLimit",
                properties.getProperty("prepStmtCacheSqlLimit")
            );
        }
        return config;
    }

    private DataSourceProvider() {}
}
