import java.sql.*;
import java.io.*;
import java.nio.file.*;
import java.math.BigDecimal;

public class HydroSparkDatabaseLoader {

    // MySQL connection info
    private static final String URL = "jdbc:mysql://localhost:3306/";
    private static final String USER = "hydro_group";  
    private static final String PASSWORD = "Hydro123!";
    private static final String CSV_FILE = "combined_output.csv"; // CSV path

    public static void main(String[] args) {
        try {
            // 0. Load MySQL JDBC driver explicitly
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 1. Connect to MySQL
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = conn.createStatement();

            // 2. Create database if it doesn't exist
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS HydroSpark");
            stmt.executeUpdate("USE HydroSpark");

            // 3. Create table for CSV data
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS Customers (
                    customer_id INT PRIMARY KEY AUTO_INCREMENT,
                    customer_name VARCHAR(100),
                    mailing_address VARCHAR(200),
                    location_id BIGINT,
                    customer_type VARCHAR(50),
                    cycle_number INT,
                    customer_phone VARCHAR(20),
                    business_name VARCHAR(100),
                    facility_name VARCHAR(100),
                    year INT,
                    month INT,
                    day INT,
                    daily_water_usage DECIMAL(10,3),
                    mailing_address_tx VARCHAR(200)
                )
                """;
            stmt.executeUpdate(createTableSQL);

            // 4. Read CSV file and insert data
            BufferedReader br = Files.newBufferedReader(Paths.get(CSV_FILE));
            String line;
            boolean firstLine = true;

            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO Customers 
                (customer_name, mailing_address, location_id, customer_type, cycle_number, customer_phone,
                 business_name, facility_name, year, month, day, daily_water_usage, mailing_address_tx)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """);

            while ((line = br.readLine()) != null) {
                if (firstLine) { // skip header row
                    firstLine = false;
                    continue;
                }

                // Split CSV line correctly handling commas inside quotes
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                // Skip lines that don't have exactly 13 fields
                if (values.length != 13) {
                    System.out.println("Skipping line (wrong number of fields): " + line);
                    continue;
                }

                // Remove surrounding quotes and trim spaces
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].replaceAll("^\"|\"$", "").trim();
                }

                // Set parameters for prepared statement
                ps.setString(1, values[0]); // Customer Name
                ps.setString(2, values[1]); // Mailing Address
                ps.setLong(3, Long.parseLong(values[2])); // Location ID
                ps.setString(4, values[3]); // Customer Type
                ps.setInt(5, Integer.parseInt(values[4])); // Cycle Number
                ps.setString(6, values[5]); // Customer Phone
                ps.setString(7, values[6]); // Business Name
                ps.setString(8, values[7]); // Facility Name
                ps.setInt(9, Integer.parseInt(values[8])); // Year
                ps.setInt(10, Integer.parseInt(values[9])); // Month
                ps.setInt(11, Integer.parseInt(values[10])); // Day

                // Handle empty Daily Water Usage safely
                BigDecimal dailyUsage = values[11].isEmpty() ? BigDecimal.ZERO : new BigDecimal(values[11]);
                ps.setBigDecimal(12, dailyUsage);

                ps.setString(13, values[12]); // Mailing Address TX

                ps.addBatch();
            }

            ps.executeBatch(); // insert all rows in batch
            System.out.println("CSV data loaded successfully into HydroSpark.Customers!");

            // Close resources
            ps.close();
            stmt.close();
            conn.close();

        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Make sure the connector jar is in the classpath.");
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
