import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;

public class ETL {
    dbLink dbLink = new dbLink();
    Connection connection_OP = dbLink.connectDB();
    Connection connection_STG = dbLink.connectSTG();
    Connection connection_DW = dbLink.connectWarehouseDB();

    public void ETLProcess() throws SQLException {
        String StaffString = "SELECT STAFF_ID, YEAR_STARTED FROM TBLSTAFF";
        String AdmissionsString = "SELECT STUDENT_ID, COURSE_ID, YEARJOINED FROM TBLADMISSIONS";
        String DepartmentsString = "SELECT DEPARTMENT_ID, DEPARTMENT_NAME, DEPARTMENT_LEAD FROM TBLDEPARTMENTS";
        String ModulesString = "SELECT MODULE_ID, MODULE_NAME, COURSE_ID, MODULE_TEACHER FROM TBLMODULES";
        String StudentBooksString = "SELECT STUDENT_ID, ISONLINE, DATERETRIEVED FROM TBLSTUDENTBOOKS";
        String CoursesString = "SELECT COURSE_ID, COURSE_NAME, DEPARTMENT_ID FROM TBLCOURSES";
        String StudentGradeString = "SELECT STUDENT_ID, MODULE_ID, GRADE, YEAR, ATTENDANCE FROM TBLSTUDENTGRADES";
        String StudentsString = "SELECT * FROM TBLSTUDENTS";

        PreparedStatement preparedStatement = connection_OP.prepareStatement(StaffString);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            int staffID = resultSet.getInt("STAFF_ID");
            Date dateStarted = resultSet.getDate("YEAR_STARTED");
            LocalDate yearStarted = dateStarted.toLocalDate();
            int year = yearStarted.getYear();

            // Check if the teacher already exists
            String checkSQL = "SELECT COUNT(*) FROM TEACHERS_DIMENSION WHERE TEACHER_ID = ?";
            PreparedStatement checkStatement = connection_STG.prepareStatement(checkSQL);
            checkStatement.setInt(1, staffID);
            ResultSet checkResult = checkStatement.executeQuery();

            if (checkResult.next() && checkResult.getInt(1) == 0) {
                // Teacher does not exist, perform the INSERT
                PreparedStatement insertStatement = connection_STG.prepareStatement("INSERT INTO TEACHERS_DIMENSION (TEACHER_ID, YEAR_STARTED) VALUES (?, ?)");
                insertStatement.setInt(1, staffID);
                insertStatement.setInt(2, year);
                insertStatement.executeUpdate();
                insertStatement.close();
            }

            checkStatement.close();
        }

        preparedStatement = connection_OP.prepareStatement(DepartmentsString);
        resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            int departmentID = resultSet.getInt("DEPARTMENT_ID");
            String departmentName = resultSet.getString("DEPARTMENT_NAME");
            String departmentLead = resultSet.getString("DEPARTMENT_LEAD");

            // Check if the department already exists
            String checkSQL = "SELECT COUNT(*) FROM DEPARTMENTS_DIMENSION WHERE DEPARTMENT_ID = ?";
            PreparedStatement checkStatement = connection_STG.prepareStatement(checkSQL);
            checkStatement.setInt(1, departmentID);
            ResultSet checkResult = checkStatement.executeQuery();

            if (checkResult.next() && checkResult.getInt(1) == 0) {
                // Department does not exist, perform the INSERT
                PreparedStatement insertStatement = connection_STG.prepareStatement("INSERT INTO DEPARTMENTS_DIMENSION (DEPARTMENT_ID, DEPARTMENT_NAME, DEPARTMENT_HEAD) VALUES (?, ?, ?)");
                insertStatement.setInt(1, departmentID);
                insertStatement.setString(2, departmentName);
                insertStatement.setString(3, departmentLead);
                insertStatement.executeUpdate();
                insertStatement.close();
            }

            checkStatement.close();
        }

        preparedStatement = connection_OP.prepareStatement(ModulesString);
        resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            int moduleID = resultSet.getInt("MODULE_ID");
            String moduleName = resultSet.getString("MODULE_NAME");
            String moduleTeacher = resultSet.getString("MODULE_TEACHER");
            int courseID = resultSet.getInt("COURSE_ID"); // Fetch COURSE_ID

            // Check if the module already exists
            String checkSQL = "SELECT MODULE_NAME, TEACHER_ID, COURSE_ID FROM MODULES_DIMENSION WHERE MODULE_ID = ?";
            PreparedStatement checkStatement = connection_STG.prepareStatement(checkSQL);
            checkStatement.setInt(1, moduleID);
            ResultSet checkResult = checkStatement.executeQuery();

            if (checkResult.next()) {
                // Module exists, check for missing fields
                String existingModuleName = checkResult.getString("MODULE_NAME");
                String existingTeacherID = checkResult.getString("TEACHER_ID");
                int existingCourseID = checkResult.getInt("COURSE_ID");

                if (existingModuleName == null || existingModuleName.isEmpty() ||
                    existingTeacherID == null || existingTeacherID.isEmpty() ||
                    existingCourseID == 0) {
                    // Update missing fields
                    String updateSQL = "UPDATE MODULES_DIMENSION SET MODULE_NAME = ?, TEACHER_ID = ?, COURSE_ID = ? WHERE MODULE_ID = ?";
                    PreparedStatement updateStatement = connection_STG.prepareStatement(updateSQL);
                    updateStatement.setString(1, moduleName);
                    updateStatement.setString(2, moduleTeacher);
                    updateStatement.setInt(3, courseID);
                    updateStatement.setInt(4, moduleID);
                    updateStatement.executeUpdate();
                    updateStatement.close();
                }
            } else {
                // Module does not exist, perform the INSERT
                PreparedStatement insertStatement = connection_STG.prepareStatement(
                        "INSERT INTO MODULES_DIMENSION (MODULE_ID, MODULE_NAME, TEACHER_ID, COURSE_ID) VALUES (?, ?, ?, ?)"
                );
                insertStatement.setInt(1, moduleID);
                insertStatement.setString(2, moduleName);
                insertStatement.setString(3, moduleTeacher);
                insertStatement.setInt(4, courseID); // Add COURSE_ID to the INSERT
                insertStatement.executeUpdate();
                insertStatement.close();
            }

            checkStatement.close();
        }
        preparedStatement = connection_OP.prepareStatement(CoursesString);
        resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            int course_ID = resultSet.getInt("COURSE_ID");
            String courseName = resultSet.getString("COURSE_NAME");
            int departmentID = resultSet.getInt("DEPARTMENT_ID");

            try {
                // Check if the course already exists
                String checkSQL = "SELECT COUNT(*) FROM COURSES_DIMENSION WHERE COURSE_ID = ?";
                try (PreparedStatement checkStatement = connection_STG.prepareStatement(checkSQL)) {
                    checkStatement.setInt(1, course_ID);
                    try (ResultSet checkResult = checkStatement.executeQuery()) {
                        if (checkResult.next() && checkResult.getInt(1) == 0) {
                            // Course does not exist, perform the INSERT
                            String insertSQL = "INSERT INTO COURSES_DIMENSION (COURSE_ID, COURSE_NAME, DEPARTMENT_ID) VALUES (?, ?, ?)";
                            try (PreparedStatement insertStatement = connection_STG.prepareStatement(insertSQL)) {
                                insertStatement.setInt(1, course_ID);
                                insertStatement.setString(2, courseName);
                                insertStatement.setInt(3, departmentID);
                                insertStatement.executeUpdate();
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace(); // Log the exception for debugging
            }
        }

        try (PreparedStatement newPreparedStatement = connection_OP.prepareStatement(StudentsString);
             ResultSet newresultSet = newPreparedStatement.executeQuery()) {

            while (newresultSet.next()) {
                int student_ID = newresultSet.getInt("STUDENT_ID");
                String STUDENT_NAME = newresultSet.getString("STUDENT_NAME");
                String degreeLevel = newresultSet.getString("DEGREELEVEL");
                boolean completedUGT = newresultSet.getBoolean("COMPLETEDUGT");

                // Check if the student already exists
                try (PreparedStatement checkStatement = connection_STG.prepareStatement("SELECT COUNT(*) FROM STUDENTS_DIMENSION WHERE STUDENT_ID = ?")) {
                    checkStatement.setInt(1, student_ID);
                    try (ResultSet checkResult = checkStatement.executeQuery()) {
                        if (checkResult.next() && checkResult.getInt(1) == 0) {
                            // Student does not exist, perform the INSERT
                            try (PreparedStatement insertStatement = connection_STG.prepareStatement(
                                    "INSERT INTO STUDENTS_DIMENSION (STUDENT_ID, STUDENT_NAME, DEGREELEVEL, COMPLETEDUGT) VALUES (?, ?, ?, ?)")) {
                                insertStatement.setInt(1, student_ID);
                                insertStatement.setString(2, STUDENT_NAME);
                                insertStatement.setString(3, degreeLevel);
                                insertStatement.setString(4, String.valueOf(completedUGT));
                                insertStatement.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String mergeSQL = "MERGE INTO STUDENT_FACT target " +
                "USING (SELECT ? AS STUDENT_ID, ? AS COURSE_ID, ? AS MODULE_ID, ? AS YEAR, ? AS BOOKSONLINE, ? AS HASSCHOLARSHIP, " +
                "? AS ATTENDANCE, ? AS GRADE, ? AS ISDROPPEDOUT, ? AS ISEMPLOYED FROM DUAL) source " +
                "ON (target.STUDENT_ID = source.STUDENT_ID) " +
                "WHEN MATCHED THEN " +
                "  UPDATE SET target.COURSE_ID = source.COURSE_ID, " +
                "              target.MODULE_ID = source.MODULE_ID, " +
                "              target.YEAR = source.YEAR, " +
                "              target.BOOKSONLINE = source.BOOKSONLINE, " +
                "              target.HASSCHOLARSHIP = source.HASSCHOLARSHIP, " +
                "              target.ATTENDANCE = source.ATTENDANCE, " +
                "              target.GRADE = source.GRADE, " +
                "              target.ISDROPPEDOUT = source.ISDROPPEDOUT, " +
                "              target.ISEMPLOYED = source.ISEMPLOYED " +
                "WHEN NOT MATCHED THEN " +
                "  INSERT (STUDENT_ID, COURSE_ID, MODULE_ID, YEAR, BOOKSONLINE, HASSCHOLARSHIP, ATTENDANCE, GRADE, ISDROPPEDOUT, ISEMPLOYED) " +
                "  VALUES (source.STUDENT_ID, source.COURSE_ID, source.MODULE_ID, source.YEAR, source.BOOKSONLINE, source.HASSCHOLARSHIP, source.ATTENDANCE, source.GRADE, source.ISDROPPEDOUT, source.ISEMPLOYED)";

        try (PreparedStatement mergeStatement = connection_STG.prepareStatement(mergeSQL);
             PreparedStatement admissionsStmt = connection_OP.prepareStatement(AdmissionsString);
             ResultSet admissionsRS = admissionsStmt.executeQuery();
             PreparedStatement booksStmt = connection_OP.prepareStatement(StudentBooksString);
             ResultSet booksRS = booksStmt.executeQuery();
             PreparedStatement gradesStmt = connection_OP.prepareStatement(StudentGradeString);
             ResultSet gradesRS = gradesStmt.executeQuery();
             PreparedStatement studentsStmt = connection_OP.prepareStatement(StudentsString);
             ResultSet studentsRS = studentsStmt.executeQuery()) {

            while (admissionsRS.next() && booksRS.next() && gradesRS.next() && studentsRS.next()) {
                int studentID = admissionsRS.getInt("STUDENT_ID");
                int courseID = admissionsRS.getInt("COURSE_ID");
                Date yearJoined = admissionsRS.getDate("YEARJOINED");
                int yearJoinedInt = yearJoined.toLocalDate().getYear();

                int moduleID = gradesRS.getInt("MODULE_ID");
                int attendance = gradesRS.getInt("ATTENDANCE");
                String grade = gradesRS.getString("GRADE");

                boolean isOnline = booksRS.getBoolean("ISONLINE");
                Date dateRetrieved = booksRS.getDate("DATERETRIEVED");
                int yearRetrieved = dateRetrieved.toLocalDate().getYear();

                boolean hasScholarship = studentsRS.getBoolean("HASSCHOLARSHIP");
                boolean isDroppedOut = studentsRS.getBoolean("ISDROPPEDOUT");
                boolean isEmployed = studentsRS.getBoolean("ISEMPLOYED");

                char scholarshipChar = hasScholarship ? 'Y' : 'N';
                char droppedOutChar = isDroppedOut ? 'Y' : 'N';
                char employedChar = isEmployed ? 'Y' : 'N';

                mergeStatement.setInt(1, studentID);
                mergeStatement.setInt(2, courseID);
                mergeStatement.setInt(3, moduleID);
                mergeStatement.setInt(4, yearJoinedInt);
                mergeStatement.setBoolean(5, isOnline);
                mergeStatement.setString(6, String.valueOf(scholarshipChar));
                mergeStatement.setInt(7, attendance);
                mergeStatement.setString(8, grade);
                mergeStatement.setString(9, String.valueOf(droppedOutChar));
                mergeStatement.setString(10, String.valueOf(employedChar));
                mergeStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
