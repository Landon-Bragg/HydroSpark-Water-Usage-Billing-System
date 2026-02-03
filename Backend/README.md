HydroSpark Water Usage & Billing System – Database Setup
This guide explains how to set up the MySQL database for the HydroSpark project, create a shared group account, and load CSV data into the database.
1. Prerequisites
macOS with MySQL installed (via Homebrew or official installer)
Java 17+ installed
MySQL JDBC Driver (mysql-connector-java-X.X.X.jar)
CSV file: combined_output.csv
2. Start MySQL Server
brew services start mysql
Check that MySQL is running:
brew services list
3. Log in as root
mysql -u root -p
Enter your root password.
4. Create HydroSpark Database
CREATE DATABASE IF NOT EXISTS HydroSpark;
USE HydroSpark;
5. Create a Shared Group Account
Replace Hydro123! with your preferred password.
CREATE USER 'hydro_group'@'%' IDENTIFIED BY 'Hydro123!';
GRANT ALL PRIVILEGES ON HydroSpark.* TO 'hydro_group'@'%';
FLUSH PRIVILEGES;
This account can be used by any team member to connect to the database.
% allows connections from any host. For local-only access, replace % with localhost.
6. Update Java Program
Edit HydroSparkDatabaseLoader.java:
private static final String USER = "hydro_group";  
private static final String PASSWORD = "Hydro123!";
private static final String CSV_FILE = "/path/to/combined_output.csv"; // update path
7. Compile Java Program
javac -cp .:mysql-connector-java-8.1.1.jar HydroSparkDatabaseLoader.java
8. Run Java Program
java -cp .:mysql-connector-java-8.1.1.jar HydroSparkDatabaseLoader
Expected output:
CSV data loaded successfully into HydroSpark.Customers!
9. Verify Data
Connect to MySQL with the group account:
mysql -u hydro_group -p
USE HydroSpark;
SELECT * FROM Customers LIMIT 5;