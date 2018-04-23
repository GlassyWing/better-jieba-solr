package org.manlier.analysis.jieba;

import org.manlier.analysis.jieba.dao.DictSource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.*;
import java.sql.Connection;
import java.util.Arrays;
import java.util.function.Consumer;

public class JiebaDBDictSource implements DictSource {
    private static final String TABLE_NAME = "jieba_dict";

    private String jdbcUrl;
    private String scanSQL;

    public JiebaDBDictSource(String jdbcUrl) {
        this(jdbcUrl, TABLE_NAME);
    }

    public JiebaDBDictSource(String jdbcUrl, String tableName) {
        this.jdbcUrl = jdbcUrl;
        this.scanSQL = "SELECT * FROM " + tableName;
    }

    @Override
    public void loadDict(Charset charset, Consumer<String[]> consumer) throws IOException {
        this.loadDict(consumer);
    }

    @Override
    public void loadDict(Consumer<String[]> consumer) throws IOException {
        System.out.println("Try to load jieba dictionary");
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            PreparedStatement statement = connection.prepareStatement(scanSQL);
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                String name = set.getString(1);
                int weight = set.getInt(2);
                String tag = set.getString(3);
                consumer.accept(new String[]{name, weight + "", tag});
            }
            System.out.println("Load jieba dictionary done!");
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        new JiebaDBDictSource("jdbc:phoenix:localhost:2181").loadDict(strings -> System.out.println(Arrays.toString(strings)));
    }
}
