public class User {
    private String username;
    private String password;


    public User(String username, String password, String email, String phone, String address, String role) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
