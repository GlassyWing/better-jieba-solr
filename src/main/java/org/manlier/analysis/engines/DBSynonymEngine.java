package org.manlier.analysis.engines;

import java.io.IOException;
import java.sql.*;
import java.util.function.Consumer;

public class DBSynonymEngine implements SynonymEngine {

    public final static String DEFAULT_TB_NAME = "thesaurus";
    public final static String DEFAULT_SYNONYMS_COLUMN = "SYNONYMS";

    private String jdbcUrl;
    private String scanSQL;

    public DBSynonymEngine(String jdbcUrl) {
        this(jdbcUrl, DEFAULT_TB_NAME, DEFAULT_SYNONYMS_COLUMN);
    }

    public DBSynonymEngine(String jdbcUrl, String tableName) {
        this(jdbcUrl, tableName, DEFAULT_SYNONYMS_COLUMN);
    }

    public DBSynonymEngine(String jdbcUrl, String tableName, String synonymsColumn) {
        this.jdbcUrl = jdbcUrl;
        scanSQL = "SELECT " + synonymsColumn + " FROM " + tableName;
    }

    public void scanThesaurus(Consumer<String> consumer) throws IOException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            PreparedStatement statement = connection.prepareStatement(scanSQL);
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                String synonyms = set.getString(1);
                consumer.accept(synonyms);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        new DBSynonymEngine("jdbc:phoenix", "THESAURUS_GROUP")
                .scanThesaurus(System.out::println);
    }
}
