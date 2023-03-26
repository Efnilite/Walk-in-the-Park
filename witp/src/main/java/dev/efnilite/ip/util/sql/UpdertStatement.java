package dev.efnilite.ip.util.sql;

import dev.efnilite.ip.IP;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Updates or inserts, depending on whether the value in the table is already set.
 */
public class UpdertStatement extends Statement {

    private final LinkedHashMap<String, Object> defaults;
    private String condition;

    public UpdertStatement(SQLManager database, String table) {
        super(database, table);
        this.defaults = new LinkedHashMap<>();
    }

    public UpdertStatement setDefault(String column, Object value) {
        defaults.put(column, value);
        return this;
    }

    public UpdertStatement setCondition(String condition) {
        this.condition = condition;
        return this;
    }

    @Override
    public void query() throws InvalidStatementException {
        if (defaults.isEmpty() || condition == null) {
            throw new IllegalArgumentException("Invalid UpdertStatement");
        }
        SelectStatement statement = new SelectStatement(manager, table);
        statement.addColumns("*").addCondition(condition);
        try {
            HashMap<String, List<Object>> map = statement.fetch();
            String key = condition.split("=")[1].replaceAll("[' ]", "");
            List<Object> objects = map != null ? map.get(key) : null;
            if (objects == null) {
                InsertStatement insert = new InsertStatement(manager, table);
                for (String skey : defaults.keySet()) {
                    insert.setValue(skey, defaults.get(skey));
                }
                insert.query();
            } else {
                UpdateStatement update = new UpdateStatement(manager, table);
                for (String skey : defaults.keySet()) {
                    update.setValue(skey, defaults.get(skey));
                }
                update.setCondition(condition);
                update.query();
            }
        } catch (SQLException ex) {
            IP.logging().stack("Error while trying to update/set values", ex);
        }
    }
}
