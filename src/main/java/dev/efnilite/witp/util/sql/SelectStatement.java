package dev.efnilite.witp.util.sql;

import dev.efnilite.witp.util.Verbose;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class SelectStatement extends Statement {

    private final List<String> columns;
    private String condition;

    public SelectStatement(Database database, String table) {
        super(database, table);
        this.columns = new ArrayList<>();
        this.condition = null;
    }

    public SelectStatement addCondition(String condition) {
        this.condition = condition;
        return this;
    }

    public SelectStatement addColumns(String... column) {
        columns.addAll(Arrays.asList(column));
        return this;
    }

    @Override
    public void query() {
        throw new IllegalStateException("Wrong method usage in SelectStatement");
    }

    /**
     * Fetches data from a table and sorts it based on the first table column.
     *
     * @return a map with key as first table column
     *
     * @throws SQLException if something goes wrong
     */
    public @Nullable LinkedHashMap<String, List<Object>> fetch() throws SQLException {
        if (columns.isEmpty()) {
            throw new InvalidStatementException("Invalid SelectStatement");
        }
        StringBuilder statement = new StringBuilder("SELECT ");
        int i = 0;
        int im = columns.size();
        for (String column : columns) {
            statement.append(column);
            i++;
            if (i != im && columns.size() > 1) {
                statement.append(", ");
            }
        }
        statement.append(" FROM ").append("`").append(table).append("`");
        if (condition != null) {
            statement.append(" WHERE ").append(condition);
        }
        statement.append(";");
        LinkedHashMap<String, List<Object>> map = new LinkedHashMap<>();

        PreparedStatement preparedStatement = database.resultQuery(statement.toString());
        if (preparedStatement == null) {
            return null;
        }
        ResultSet set = preparedStatement.executeQuery();
        if (set == null) {
            return null;
        }

        while (set.next()) {
            String key = set.getString(1);
            List<Object> values = new ArrayList<>();
            if (columns.size() > 1) {
                for (int j = 0; j < columns.size(); j++) {
                    if (j == columns.size() - 1) {
                        values.add(set.getString(j + 1));
                        continue;
                    }
                    values.add(set.getString(j + 2));
                }
            } else {
                values.add(key);
            }
            map.put(key, values);
        }
        preparedStatement.close();
        set.close();
        return map;
    }
}
