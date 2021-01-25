package dev.efnilite.witp.util.sql;

import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SelectStatement extends Statement {

    private final List<String> columns;

    public SelectStatement(Database database, String table) {
        super(database, table);
        this.columns = new ArrayList<>();
    }

    public SelectStatement addColumn(String column) {
        columns.add(column);
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

    public @Nullable HashMap<String, Object> fetch() throws SQLException {
        if (columns.size() == 0) {
            throw new InvalidStatementException("Invalid SelectStatement");
        }
        StringBuilder statement = new StringBuilder("SELECT ");
        int i = 0;
        int im = columns.size();
        for (String column : columns) {
            statement.append("`").append(column).append("`");
            i++;
            if (i != im) {
                statement.append(", ");
            }
        }
        statement.append(" FROM `").append(table).append("`;");
        ResultSet set = database.resultQuery(statement.toString());
        HashMap<String, Object> map = new HashMap<>();
        if (set == null) {
            return null;
        }
        for (String column : columns) {
            if (set.next()) {
                map.put(column, set.getString(column));
            }
        }
        return map;
    }
}
