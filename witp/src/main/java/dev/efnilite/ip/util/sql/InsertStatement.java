package dev.efnilite.ip.util.sql;

import java.util.LinkedHashMap;

public class InsertStatement extends Statement {

    private final LinkedHashMap<String, Object> values;

    public InsertStatement(SQLManager database, String table) {
        super(database, table);
        this.values = new LinkedHashMap<>();
    }

    public InsertStatement setValue(String column, Object value) {
        values.put(column, value);
        return this;
    }

    @Override
    public void query() {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Invalid InsertStatement");
        }
        StringBuilder statement = new StringBuilder("INSERT INTO `" + table + "` (");
        int i = 0;
        int im = values.keySet().size();
        for (String key : values.keySet()) {
            statement.append("`").append(key).append("`");
            i++;
            if (i != im) {
                statement.append(", ");
            }
        }
        statement.append(") VALUES (");
        i = 0;
        for (String key : values.keySet()) {
            Object value = values.get(key);
            if (value instanceof Number || value instanceof Boolean) {
                statement.append(value);
            } else {
                statement.append("'").append(value).append("'");
            }
            i++;
            if (i != im) {
                statement.append(", ");
            }
        }
        statement.append(");");
        manager.sendQuery(statement.toString());
    }
}
