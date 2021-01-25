/*
 * Copyright © 2017-2020 Ocado (Ocava)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocadotechnology.tableio.sql;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.tableio.TableLine;

public class SQLiteConnection implements AutoCloseable {
    private static final String SQL_SERVER_STRING = "jdbc:sqlite:";

    private final Connection conn;

    private SQLiteConnection(Connection conn) {
        this.conn = conn;
    }

    /**
     * Creates a SQLiteConnection from a {@link File} using {@link #create(String)}.
     *
     * @param file the file to create the SQLiteConnection from.
     * @return the SQLiteConnection.
     * @throws SQLException if the SQL Connection failed to connect.
     */
    public static SQLiteConnection create(File file) throws SQLException {
        return create(file.toPath());
    }

    /**
     * Creates a SQLiteConnection from a {@link Path} using {@link #create(String)}.
     *
     * @param path the path to create the SQLiteConnection from.
     * @return the SQLiteConnection.
     * @throws SQLException if the SQL Connection failed to connect.
     */
    public static SQLiteConnection create(Path path) throws SQLException {
        return create(path.toString());
    }

    /**
     * Creates a SQLiteConnection from a fileName concatenated with a SQL server String. Throws an {@link SQLException}
     * if the connection failed to be created.
     *
     * @param fileName the fileName to create the SQLiteConnection from.
     * @return the SQLiteConnection.
     * @throws SQLException if the SQL Connection failed to connect.
     */
    public static SQLiteConnection create(String fileName) throws SQLException {
        Connection conn = DriverManager.getConnection(SQL_SERVER_STRING + fileName);

        if (conn == null) {
            throw new SQLException("Failed to create SQL connection");
        } else {
            return new SQLiteConnection(conn);
        }
    }

    /**
     * Checks if a specified table exists in a database. Throws an {@link SQLException}
     *
     * @param tableName the tableName
     * @return true if the table exists. false if it does not.
     * @throws SQLException if the query failed to execute.
     */
    public boolean tableExists(String tableName) throws SQLException {
        AtomicBoolean tableExists = new AtomicBoolean();

        executeQuery(
                result -> tableExists.set(isCountOne(result)),
                "SELECT COUNT(*) AS count",
                "FROM sqlite_master",
                "WHERE type='table'",
                "AND name='" + tableName + "'");

        return tableExists.get();
    }

    /**
     * Creates a table if that table with specified columns if it does not exist. Throws an {@link SQLException}
     *
     * @param tableName the table to create.
     * @param header    the headers to create.
     * @throws SQLException if the table failed to be created.
     */
    public void createTable(String tableName, ImmutableSet<String> header) throws SQLException {
        boolean tableExists = tableExists(tableName);

        if (!tableExists) {
            execute(
                    "CREATE TABLE " + tableName + " (",
                    String.join(",\n", header),
                    ")"
            );
        }
    }

    /**
     * Insert an entry into a table in the SQL database. Throws an {@link SQLException}
     *
     * @param tableName the table to insert the entry to.
     * @param tableLine the entry to insert into the table.
     * @throws SQLException if the query failed to execute.
     */
    public void insertEntry(String tableName, TableLine tableLine) throws SQLException {
        execute(getInsertSQL(tableName, tableLine));
    }

    /**
     * Insert a stream of entries into a table in the SQL database. Throws an {@link SQLException}
     *
     * @param tableName        the table to insert the entries to.
     * @param tableLinesStream the entries to insert into the table.
     * @throws SQLException If the query failed to execute.
     */
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "known bug in spotbugs - https://github.com/spotbugs/spotbugs/issues/259")
    public void insertEntries(String tableName, Stream<TableLine> tableLinesStream) throws SQLException {
        ImmutableList<TableLine> tableLines = tableLinesStream.collect(ImmutableList.toImmutableList());

        try (Statement statement = conn.createStatement()) {
            statement.execute("BEGIN");
            for (TableLine line : tableLines) {
                statement.addBatch(getInsertSQL(tableName, line));
            }

            statement.executeBatch();
            statement.execute("COMMIT");
        }
    }

    /**
     * Consumes a specific sql table. Throws an {@link SQLException}
     *
     * @param tableName      the tableName of the table to consume to consume
     * @param resultConsumer the consumer to consume the results.
     * @throws SQLException if the query failed to execute.
     */
    public void consumeTable(String tableName, Consumer<ResultSet> resultConsumer) throws SQLException {
        String sql = "SELECT * from " + tableName;

        executeQuery(resultConsumer, sql);
    }

    /**
     * @param sql the SQL commands to execute
     * @throws SQLException if the commands failed to execute.
     */
    public void execute(String... sql) throws SQLException {
        String sqlJoined = String.join("\n", sql);

        try (PreparedStatement statement = conn.prepareStatement(sqlJoined)) {
            statement.execute();
        }
    }

    /**
     * Executes an SQL query and then consumes the result using a {@link Consumer} or {@link ResultSet}. Throws an {@link SQLException}
     *
     * @param resultConsumer the resultConsumer to consume the result of the SQL query.
     * @param sql            the sql queries to execute.
     * @throws SQLException if the query failed to execute
     */
    public void executeQuery(Consumer<ResultSet> resultConsumer, String... sql) throws SQLException {
        String sqlJoined = String.join("\n", sql);

        try (
                PreparedStatement statement = conn.prepareStatement(sqlJoined);
                ResultSet result = statement.executeQuery()) {
            resultConsumer.accept(result);
        }
    }

    /**
     * Closes the SQLiteConnection. Throws an {@link SQLException}.
     *
     * @throws SQLException if error with SQLiteConnection closer occurred.
     */
    @Override
    public void close() throws SQLException {
        conn.close();
    }

    private boolean isCountOne(ResultSet result) {
        try {
            return result.getInt("count") == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getInsertSQL(String tableName, TableLine line) {
        String valueString = String.join(",\n", line.getLineMapWithStringsQuoted().values());

        return String.join(
                "\n",
                "INSERT INTO " + tableName + " VALUES (",
                valueString,
                ")"
        );
    }
}