package dev.efnilite.witp.util.sql;

import java.sql.SQLException;

public class InvalidStatementException extends SQLException {

    public InvalidStatementException(String reason) {
        super(reason);
    }
}