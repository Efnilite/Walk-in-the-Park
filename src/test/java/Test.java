import dev.efnilite.witp.util.sql.SelectStatement;

import java.sql.SQLException;

public class Test {

    public static void main(String[] args) throws SQLException {
        SelectStatement statement = new SelectStatement(null, "main");
        statement.addColumns("uuid", "name", "highscore", "hstime");
        statement.fetch();
    }
}