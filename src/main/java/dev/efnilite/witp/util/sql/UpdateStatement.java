package dev.efnilite.witp.util.sql;

import dev.efnilite.fycore.sql.InvalidStatementException;

import java.util.LinkedHashMap;

public class UpdateStatement extends Statement {

    private String condition;
    private final LinkedHashMap<String, Object> values;

    public UpdateStatement(Database database, String table) {
        super(database, table);
        this.values = new LinkedHashMap<>();
    }

    public UpdateStatement setCondition(String condition) {
        this.condition = condition;
        return this;
    }

    public UpdateStatement setValue(String column, Object value) {
        values.put(column, value);
        return this;
    }

    @Override
    public void query() throws InvalidStatementException, InvalidStatementException {
        if (condition == null || values.isEmpty()) {
            throw new InvalidStatementException("Invalid UpdateStatement");
        }
        StringBuilder statement = new StringBuilder("UPDATE `" + table + "` SET ");
        int i = 0;
        int im = values.keySet().size();
        for (String key : values.keySet()) {
            statement.append(key).append(" = ");
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
        statement.append(" WHERE ").append(condition).append(";");
        database.query(statement.toString());
    }
}
