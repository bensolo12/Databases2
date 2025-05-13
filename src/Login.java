import java.sql.*;

public class Login {

    public int authenticate(String userID, String password) {
        Connection connection = new dbLink().connectDB();
        try {
            String SQLString = "SELECT role_id FROM TBLSTAFF WHERE STAFF_ID = ? AND password = ?";
            PreparedStatement statement = connection.prepareStatement(SQLString);
            statement.setString(1, userID);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("role_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
