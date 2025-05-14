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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

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
        JButton booksGraphButton = new JButton("Books Retrieved Report");
        booksGraphButton.addActionListener(e -> {
            try {
                showBooksRetrievedGraph(mainFrame);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            role = 0;
            showFrame();
        });
        container.add(booksGraphButton);
        container.add(logoutButton);
        return container;
    }

    private void showBooksRetrievedGraph(JFrame mainFrame) throws SQLException {
        // Fetch the data
        Map<Integer, int[]> report = generateBooksRetrievedReport();

        // Create the dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<Integer, int[]> entry : report.entrySet()) {
            int year = entry.getKey();
            int onlineCount = entry.getValue()[0];
            int inPersonCount = entry.getValue()[1];

            dataset.addValue(onlineCount, "Online", String.valueOf(year));
            dataset.addValue(inPersonCount, "In Person", String.valueOf(year));
        }

        // Create the chart
        JFreeChart lineChart = ChartFactory.createLineChart(
                "Books Retrieved by Year",
                "Year",
                "Number of Books",
                dataset
        );

        // Display the chart in a panel
        ChartPanel chartPanel = new ChartPanel(lineChart);
        chartPanel.setPreferredSize(new Dimension(800, 600));

        mainFrame.getContentPane().removeAll();
        mainFrame.add(chartPanel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private Map<Integer, int[]> generateBooksRetrievedReport() throws SQLException {
        dbLink db = new dbLink();
        Connection connection = db.connectSTG();

        String SQLString = "SELECT YEAR, " +
                "SUM(BOOKSONLINE) AS ONLINE_COUNT, " +
                "SUM(BOOKSINPERSON) AS IN_PERSON_COUNT " +
                "FROM STUDENT_FACT " +
                "GROUP BY YEAR";

        PreparedStatement statement = connection.prepareStatement(SQLString);
        ResultSet resultSet = statement.executeQuery();

        Map<Integer, int[]> report = new HashMap<>();
        while (resultSet.next()) {
            int year = resultSet.getInt("YEAR");
            int onlineCount = resultSet.getInt("ONLINE_COUNT");
            int inPersonCount = resultSet.getInt("IN_PERSON_COUNT");

            report.put(year, new int[]{onlineCount, inPersonCount});
        }

        return report;
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
                        showAdmissionsTable(mainFrame, courseId);
                    }
                });

                container.add(courseComboBox);
                container.revalidate();
                container.repaint();

            } else if ("Postgrad Students Report".equals(selectedReport)) {
                container.removeAll();
                //Combo box for department selection, with a table that shows the year and number of postgrad students that year
                JComboBox<String> departmentComboBox = new JComboBox<>();
                departmentComboBox.addItem("Select Department");
                try {
                    ArrayList<Integer> departments = getDepartments();
                    for (Integer department : departments) {
                        departmentComboBox.addItem(department.toString());
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                departmentComboBox.addActionListener(event -> {
                    String selectedDepartment = (String) departmentComboBox.getSelectedItem();
                    if (selectedDepartment != null && !selectedDepartment.equals("Select Department")) {
                        int departmentId = Integer.parseInt(selectedDepartment);
                        try {
                            showPostgradTable(mainFrame, departmentId);
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });
                container.add(departmentComboBox);
                container.revalidate();
                container.repaint();
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

    private void showPostgradTable(JFrame mainFrame, int departmentId) throws SQLException {
        // Create a panel to hold components
        JPanel panel = new JPanel(new BorderLayout());

        // Create a table model and set up the table
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Year", "Postgrad Students"}, 0);
        JTable table = new JTable(tableModel);

        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Fetch the report for the selected department
        Map<Integer, Integer> report = generatePostgradReport(departmentId);

        // Clear the table and populate it with new data
        tableModel.setRowCount(0);
        if (report != null) {
            for (Map.Entry<Integer, Integer> entry : report.entrySet()) {
                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        }

        mainFrame.getContentPane().removeAll();
        mainFrame.add(panel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private Map<Integer, Integer> generatePostgradReport(int departmentId) throws SQLException {
        dbLink db = new dbLink();
        Connection connection = db.connectSTG();

        String SQLString = "SELECT sf.YEAR, COUNT(sf.STUDENT_ID) AS POSTGRAD_COUNT " +
                "FROM STUDENT_FACT sf " +
                "JOIN COURSES_DIMENSION cd ON sf.COURSE_ID = cd.COURSE_ID " +
                "JOIN STUDENTS_DIMENSION sd ON sf.STUDENT_ID = sd.STUDENT_ID " +
                "WHERE cd.DEPARTMENT_ID = ? AND sd.DEGREELEVEL = 'P' AND sd.COMPLETEDUGT = 'true' " +
                "GROUP BY sf.YEAR";

        PreparedStatement statement = connection.prepareStatement(SQLString);
        statement.setInt(1, departmentId);

        ResultSet resultSet = statement.executeQuery();

        Map<Integer, Integer> report = new HashMap<>();
        while (resultSet.next()) {
            int year = resultSet.getInt("YEAR");
            int count = resultSet.getInt("POSTGRAD_COUNT");

            report.put(year, count);
        }

        return report;
    }

    public void showAdmissionsTable(JFrame frame, int selectedCourse) {
        // Create a panel to hold components
        JPanel panel = new JPanel(new BorderLayout());

        // Create a table model and set up the table
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Year", "New Students"}, 0);
        JTable table = new JTable(tableModel);

        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        try {
            // Fetch the report for the selected course
            Map<Integer, Integer> report = generateAdmissionsReport(selectedCourse);

            // Clear the table and populate it with new data
            tableModel.setRowCount(0);
            if (report != null) {
                for (Map.Entry<Integer, Integer> entry : report.entrySet()) {
                    tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        frame.getContentPane().removeAll();
        frame.add(panel);
        frame.revalidate();
        frame.repaint();
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

    private ArrayList<Integer> getDepartments() throws SQLException {
        dbLink db = new dbLink();
        Connection connection = db.connectSTG();
        String SQLString = "SELECT DISTINCT DEPARTMENT_ID FROM DEPARTMENTS_DIMENSION";
        PreparedStatement statement = connection.prepareStatement(SQLString);
        ResultSet resultSet = statement.executeQuery();

        ArrayList<Integer> deps = new ArrayList<>();
        while (resultSet.next()) {
            deps.add(resultSet.getInt("DePARTMENT_ID"));
        }
        return deps;
    }

    public void showScholarshipTable(JFrame frame, ArrayList<Integer> years) {
        JPanel panel = new JPanel(new BorderLayout());

        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Course ID", "Scholarships"}, 0);
        JTable table = new JTable(tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JComboBox<Integer> yearComboBox = new JComboBox<>(years.toArray(new Integer[0]));
        yearComboBox.addActionListener(e -> {
            int selectedYear = (int) yearComboBox.getSelectedItem();
            try {
                Map<Integer, Map<Integer, Integer>> report = generateScholarshipReport(new ArrayList<>(years));
                Map<Integer, Integer> yearData = report.get(selectedYear);

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


        panel.add(yearComboBox, BorderLayout.NORTH);
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

    private Map<Integer, Integer> generateAdmissionsReport(int selectedCourseID) throws SQLException {
        dbLink db = new dbLink();
        Connection connection = db.connectSTG();

        String SQLString = "SELECT YEAR, COUNT(STUDENT_ID) AS STUDENT_COUNT " +
                "FROM STUDENT_FACT WHERE COURSE_ID = ? " +
                "GROUP BY YEAR";

        PreparedStatement statement = connection.prepareStatement(SQLString);
        statement.setInt(1, selectedCourseID);

        ResultSet resultSet = statement.executeQuery();

        Map<Integer, Integer> report = new HashMap<>();
        while (resultSet.next()) {
            int year = resultSet.getInt("YEAR");
            int count = resultSet.getInt("STUDENT_COUNT");

            report.put(year, count);
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
        JButton departmentGradesButton = new JButton("Department Grades Report");
        departmentGradesButton.addActionListener(e -> {
            try {
                showDepartmentGradesReport(mainFrame);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        container.add(departmentGradesButton);
        container.add(logoutButton);
        return container;
    }

    private void showDepartmentGradesReport(JFrame mainFrame) throws SQLException {
        // Fetch the data
        Map<String, Map<Integer, Double>> report = generateDepartmentGradesReport();

        // Create a panel to hold the table and graph
        JPanel panel = new JPanel(new BorderLayout());

        // Create the table
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Department", "Year", "Average Grade"}, 0);
        JTable table = new JTable(tableModel);

        // Populate the table with data
        for (Map.Entry<String, Map<Integer, Double>> departmentEntry : report.entrySet()) {
            String department = departmentEntry.getKey();
            for (Map.Entry<Integer, Double> yearEntry : departmentEntry.getValue().entrySet()) {
                int year = yearEntry.getKey();
                double avgGrade = yearEntry.getValue();
                tableModel.addRow(new Object[]{department, year, avgGrade});
            }
        }

        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create the dataset for the graph
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Map<Integer, Double>> departmentEntry : report.entrySet()) {
            String department = departmentEntry.getKey();
            for (Map.Entry<Integer, Double> yearEntry : departmentEntry.getValue().entrySet()) {
                int year = yearEntry.getKey();
                double avgGrade = yearEntry.getValue();
                dataset.addValue(avgGrade, department, String.valueOf(year));
            }
        }

        // Create the line chart
        JFreeChart lineChart = ChartFactory.createLineChart(
                "Average Grades by Department and Year",
                "Year",
                "Average Grade",
                dataset
        );

        // Add the chart to a panel
        ChartPanel chartPanel = new ChartPanel(lineChart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        panel.add(chartPanel, BorderLayout.SOUTH);

        // Display the panel in the main frame
        mainFrame.getContentPane().removeAll();
        mainFrame.add(panel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private Map<String, Map<Integer, Double>> generateDepartmentGradesReport() throws SQLException {
        dbLink db = new dbLink();
        Connection connection = db.connectSTG();

        String SQLString = "SELECT dd.DEPARTMENT_NAME, sf.YEAR, AVG(sf.GRADE) AS AVG_GRADE " +
                "FROM STUDENT_FACT sf " +
                "JOIN COURSES_DIMENSION cd ON sf.COURSE_ID = cd.COURSE_ID " +
                "JOIN DEPARTMENTS_DIMENSION dd ON cd.DEPARTMENT_ID = dd.DEPARTMENT_ID " +
                "GROUP BY dd.DEPARTMENT_NAME, sf.YEAR " +
                "ORDER BY dd.DEPARTMENT_NAME, sf.YEAR";

        PreparedStatement statement = connection.prepareStatement(SQLString);
        ResultSet resultSet = statement.executeQuery();

        Map<String, Map<Integer, Double>> report = new HashMap<>();
        while (resultSet.next()) {
            String department = resultSet.getString("DEPARTMENT_NAME");
            int year = resultSet.getInt("YEAR");
            double avgGrade = resultSet.getDouble("AVG_GRADE");

            report.computeIfAbsent(department, k -> new HashMap<>()).put(year, avgGrade);
        }

        return report;
    }

    public Container departmentHeadContainer(){
        Container container = new Container();
        container.setLayout(new GridLayout(3, 2));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            role = 0;
            showFrame();
        });
        JComboBox<String> reportsComboBox = new JComboBox<>();
        reportsComboBox.addItem("Select Report");
        reportsComboBox.addItem("Module Passes By Teacher");
        reportsComboBox.addItem("Course Drop outs per year");
        reportsComboBox.addItem("Employed Students Within 2 Years");
        reportsComboBox.addItem("Staff Employment Length");
        reportsComboBox.addActionListener(e -> {
            String selectedReport = (String) reportsComboBox.getSelectedItem();
            if ("Staff Employment Length".equals(selectedReport)) {
                container.removeAll();
                try {
                    showStaffEmploymentLength(mainFrame);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            else if ("Course Drop outs per year".equals(selectedReport)) {
                container.removeAll();
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
                        try {
                            showCourseDropOuts(mainFrame, courseId);
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });

                container.add(courseComboBox);
                container.revalidate();
                container.repaint();
            }
            else if ("Module Passes By Teacher".equals(selectedReport)) {
                container.removeAll();
                //Combo box for teacher selection, with a table that shows the year and number of new students that year
                JComboBox<String> teacherComboBox = new JComboBox<>();
                teacherComboBox.addItem("Select Teacher");
                try {
                    ArrayList<Integer> teachers = getCourses();
                    for (Integer teacher : teachers) {
                        teacherComboBox.addItem(teacher.toString());
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                teacherComboBox.addActionListener(event -> {
                    String selectedTeacher = (String) teacherComboBox.getSelectedItem();
                    if (selectedTeacher != null && !selectedTeacher.equals("Select Teacher")) {
                        int teacherId = Integer.parseInt(selectedTeacher);
                        showAdmissionsTable(mainFrame, teacherId);
                    }
                });

                container.add(teacherComboBox);
                container.revalidate();
                container.repaint();

            }
        });


        container.add(reportsComboBox);
        container.add(logoutButton);
        return container;
    }

    private void showCourseDropOuts(JFrame mainFrame, int courseId) throws SQLException {
        // Create a panel to hold components
        JPanel panel = new JPanel(new BorderLayout());

        // Create a table model and set up the table
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Year", "Drop Outs"}, 0);
        JTable table = new JTable(tableModel);

        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Fetch the report for the selected course
        Map<Integer, Integer> report = generateCourseDropOutsReport(courseId);

        // Clear the table and populate it with new data
        tableModel.setRowCount(0);
        if (report != null) {
            for (Map.Entry<Integer, Integer> entry : report.entrySet()) {
                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        }

        mainFrame.getContentPane().removeAll();
        mainFrame.add(panel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private Map<Integer, Integer> generateCourseDropOutsReport(int courseId) throws SQLException {
        dbLink db = new dbLink();
        Connection connection = db.connectSTG();

        String SQLString = "SELECT YEAR, COUNT(STUDENT_ID) AS DROP_OUT_COUNT " +
                "FROM STUDENT_FACT WHERE COURSE_ID = ? AND ISDROPPEDOUT = 'Y' " +
                "GROUP BY YEAR";

        PreparedStatement statement = connection.prepareStatement(SQLString);
        statement.setInt(1, courseId);

        ResultSet resultSet = statement.executeQuery();

        Map<Integer, Integer> report = new HashMap<>();
        while (resultSet.next()) {
            int year = resultSet.getInt("YEAR");
            int count = resultSet.getInt("DROP_OUT_COUNT");

            report.put(year, count);
        }

        return report;
    }

    private void showStaffEmploymentLength(JFrame mainFrame) throws SQLException {
        // Fetch the data
        Map<Integer, Integer> report = generateStaffEmploymentLengthReport();

        // Create a panel to hold the table
        JPanel panel = new JPanel(new BorderLayout());

        // Create the table
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Teacher ID", "Years Employed"}, 0);
        JTable table = new JTable(tableModel);

        // Populate the table with data
        for (Map.Entry<Integer, Integer> entry : report.entrySet()) {
            int teacherId = entry.getKey();
            int yearsEmployed = entry.getValue();
            tableModel.addRow(new Object[]{teacherId, yearsEmployed});
        }

        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Display the panel in the main frame
        mainFrame.getContentPane().removeAll();
        mainFrame.add(panel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private Map<Integer, Integer> generateStaffEmploymentLengthReport() throws SQLException {
        dbLink db = new dbLink();
        Connection connection = db.connectSTG();

        String SQLString = "SELECT TEACHER_ID, YEAR_STARTED FROM TEACHERS_DIMENSION";

        PreparedStatement statement = connection.prepareStatement(SQLString);
        ResultSet resultSet = statement.executeQuery();

        Map<Integer, Integer> report = new HashMap<>();
        int currentYear = java.time.Year.now().getValue();

        while (resultSet.next()) {
            int teacherId = resultSet.getInt("TEACHER_ID");
            int yearStarted = resultSet.getInt("YEAR_STARTED");
            int yearsEmployed = currentYear - yearStarted;

            report.put(teacherId, yearsEmployed);
        }

        return report;
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
