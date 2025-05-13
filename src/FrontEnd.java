import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class FrontEnd {
    JFrame mainFrame = new JFrame();
    int role = 0;
    Container loginContainer;
    public Container loginContainer(){
        Login login = new Login();

        Container container = new Container();
        container.setLayout(new GridLayout(3, 2));
        JLabel usernameLabel = new JLabel("User ID");
        JTextField usernameField = new JTextField();
        JLabel passwordLabel = new JLabel("Password");
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
                    String userID = usernameField.getText();
                    String password = new String(passwordField.getPassword());
            role = login.authenticate(userID, password);
            showFrame();
                });
        container.add(usernameLabel);
        container.add(usernameField);
        container.add(passwordLabel);
        container.add(passwordField);
        container.add(loginButton);
        return container;
    }
    public Container adminContainer(){
        Container container = new Container();
        container.setLayout(new GridLayout(3, 2));
        JButton ETLButton = new JButton("ETL");
        ETLButton.addActionListener(e -> {
            ETL etl = new ETL();
            try {
                etl.ETLProcess();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            role = 0;
            showFrame();
        });
        container.add(logoutButton);
        container.add(ETLButton);
        return container;
    }

    public Container chiefLibrarianContainer(){
        Container container = new Container();
        container.setLayout(new GridLayout(3, 2));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            role = 0;
            showFrame();
        });
        container.add(logoutButton);
        return container;
    }

    public Container admissionFinanceDirectorContainer(){
        Container container = new Container();
        container.setSize(500, 500);
        container.setLayout(new GridLayout(3, 2));
        JComboBox<String> reportsComboBox = new JComboBox<>();
        reportsComboBox.addItem("Select Report");
        reportsComboBox.addItem("Scholarship Report");
        reportsComboBox.addItem("New Students Report");
        reportsComboBox.addItem("Postgrad Students Report");
        reportsComboBox.addActionListener(e -> {
            String selectedReport = (String) reportsComboBox.getSelectedItem();
            if ("Scholarship Report".equals(selectedReport)) {
                container.removeAll();
                JTextField yearField1 = new JTextField("Enter Year 1");
                JTextField yearField2 = new JTextField("Enter Year 2");

                JButton scholarshipButton = new JButton("Generate Report");
                scholarshipButton.addActionListener(_ -> {
                    try {
                        ArrayList<Integer> years = new ArrayList<>();
                        int totalYears = Integer.parseInt(yearField2.getText()) - Integer.parseInt(yearField1.getText());

                        for (int i = 0; i <= totalYears; i++) {
                            years.add(Integer.parseInt(yearField1.getText()) + i);
                        }
                        showScholarshipTable(mainFrame, years);

                    } catch (NumberFormatException ex) {
                        throw new RuntimeException(ex);
                    }
                });


                container.add(yearField1);
                container.add(yearField2);
                container.add(scholarshipButton);

                // Revalidate and repaint the container to reflect changes
                container.revalidate();
                container.repaint();
            } else if ("New Students Report".equals(selectedReport)) {
                container.removeAll();
                //Combo box for course selection, with a table that shows the year and number of new students that year
                JComboBox<String> courseComboBox = new JComboBox<>();
                courseComboBox.addItem("Select Course");
                try {
                    ArrayList<Integer> courses = getCourses();
                    for (Integer course : courses) {
                        courseComboBox.addItem(course.toString());
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                courseComboBox.addActionListener(event -> {
                    String selectedCourse = (String) courseComboBox.getSelectedItem();
                    if (selectedCourse != null && !selectedCourse.equals("Select Course")) {
                        int courseId = Integer.parseInt(selectedCourse);
                        // Fetch and display the new students report for the selected course
                        // You can implement this part based on your database structure

                    }
                });

                container.add(courseComboBox);
                container.revalidate();
                container.repaint();

            } else if ("Postgrad Students Report".equals(selectedReport)) {
                // Handle postgrad students report
            }
        });


        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            role = 0;
            showFrame();
        });
        container.add(reportsComboBox);
        container.add(logoutButton);

        return container;
    }

    private ArrayList<Integer> getCourses() throws SQLException {
        dbLink db = new dbLink();
        Connection connection = db.connectSTG();
        String SQLString = "SELECT DISTINCT COURSE_ID FROM STUDENT_FACT";
        PreparedStatement statement = connection.prepareStatement(SQLString);
        ResultSet resultSet = statement.executeQuery();

        ArrayList<Integer> courses = new ArrayList<>();
        while (resultSet.next()) {
            courses.add(resultSet.getInt("COURSE_ID"));
        }
        return courses;
    }

    public void showScholarshipTable(JFrame frame, ArrayList<Integer> years) {
        // Create a panel to hold components
        JPanel panel = new JPanel(new BorderLayout());

        // Create a table model and set up the table
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Course ID", "Scholarships"}, 0);
        JTable table = new JTable(tableModel);

        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create a combo box for year selection
        JComboBox<Integer> yearComboBox = new JComboBox<>(years.toArray(new Integer[0]));
        yearComboBox.addActionListener(e -> {
            int selectedYear = (int) yearComboBox.getSelectedItem();
            try {
                // Fetch the report for the selected year
                Map<Integer, Map<Integer, Integer>> report = generateScholarshipReport(new ArrayList<>(years));
                Map<Integer, Integer> yearData = report.get(selectedYear);

                // Clear the table and populate it with new data
                tableModel.setRowCount(0);
                if (yearData != null) {
                    for (Map.Entry<Integer, Integer> entry : yearData.entrySet()) {
                        tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        // Add the combo box to the panel
        panel.add(yearComboBox, BorderLayout.NORTH);

        // Add the panel to the frame
        frame.getContentPane().removeAll();
        frame.add(panel);
        frame.revalidate();
        frame.repaint();
    }

    private Map<Integer, Map<Integer, Integer>> generateScholarshipReport(ArrayList<Integer> years) throws SQLException {
        dbLink db = new dbLink();
        Connection connection = db.connectSTG();

        // Dynamically build placeholders for IN clause
        String placeholders = years.stream()
                .map(y -> "?")
                .collect(Collectors.joining(", "));

        String SQLString = "SELECT YEAR, COURSE_ID, COUNT(STUDENT_ID) AS SCHOLARSHIP_COUNT " +
                "FROM STUDENT_FACT WHERE HASSCHOLARSHIP = 'Y' AND YEAR IN (" + placeholders + ") " +
                "GROUP BY YEAR, COURSE_ID";

        PreparedStatement statement = connection.prepareStatement(SQLString);

        // Set each year as a parameter
        for (int i = 0; i < years.size(); i++) {
            statement.setInt(i + 1, years.get(i));
        }

        ResultSet resultSet = statement.executeQuery();

        Map<Integer, Map<Integer, Integer>> report = new HashMap<>();
        while (resultSet.next()) {
            int year = resultSet.getInt("YEAR");
            int courseId = resultSet.getInt("COURSE_ID");
            int count = resultSet.getInt("SCHOLARSHIP_COUNT");

            report.computeIfAbsent(year, k -> new HashMap<>()).put(courseId, count);
        }

        return report;
    }


    public Container viceChancellorContainer(){
        Container container = new Container();
        container.setLayout(new GridLayout(3, 2));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            role = 0;
            showFrame();
        });
        container.add(logoutButton);
        return container;
    }

    public Container departmentHeadContainer(){
        Container container = new Container();
        container.setLayout(new GridLayout(3, 2));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            role = 0;
            showFrame();
        });
        container.add(logoutButton);
        return container;
    }

    public void showFrame() {
        mainFrame.getContentPane().removeAll(); // Clear previous content

        if (role == 0) {
            loginContainer = loginContainer();
            mainFrame.add(loginContainer);
        } else {
            if (loginContainer != null) {
                mainFrame.remove(loginContainer);
            }
        }

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(300, 300);
        mainFrame.setResizable(false);

        String frameTitle = "Login";
        if (role == 100) {
            frameTitle = "Administrator";
            mainFrame.add(adminContainer());
        } else if (role == 1) {
            frameTitle = "Department Head";
            mainFrame.add(departmentHeadContainer());
        } else if (role == 2) {
            frameTitle = "Chief Librarian";
            mainFrame.add(chiefLibrarianContainer());
        } else if (role == 3) {
            frameTitle = "Admission/Finance Director";
            mainFrame.add(admissionFinanceDirectorContainer());
        } else if (role == 4) {
            frameTitle = "Vice Chancellor";
            mainFrame.add(viceChancellorContainer());
        }

        mainFrame.setTitle(frameTitle);
        mainFrame.setSize(1000, 1000);
        mainFrame.revalidate();
        mainFrame.repaint();
        mainFrame.setVisible(true);
    }
}
