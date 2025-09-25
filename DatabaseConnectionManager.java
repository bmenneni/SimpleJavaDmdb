import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionManager {
	private static Connection conn;
	private static final String databaseUrl = "jdbc:sqlite:duelmasters.db";
	
	public static Connection getConnection() throws SQLException {
		try {
			if(conn==null||conn.isClosed()) {
				conn = DriverManager.getConnection(databaseUrl);
			}
		} catch(SQLException sqle) {
			throw new SQLException("Failed to open database connection.", sqle);
		}
		return conn;
	}
}