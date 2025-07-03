# MySQL Database Setup Instructions

## Prerequisites
1. Install MySQL Server on your system
2. Make sure MySQL service is running

## Database Configuration
The application is configured to connect to MySQL with the following default settings:
- **Host**: localhost
- **Port**: 3306
- **Database**: todo_app (will be created automatically)
- **Username**: root
- **Password**: root

## Setup Steps

### 1. Install MySQL
- Download and install MySQL Server from https://dev.mysql.com/downloads/mysql/
- During installation, set a root password or leave it empty
- Make sure to start the MySQL service

### 2. Update Database Configuration (if needed)
If your MySQL setup is different, update the following file:
`src/main/java/org/example/demo/config/DatabaseConfig.java`

Change these constants:
```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/todo_app?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "root"; // Set your password here
```

### 3. Database Creation
The application will automatically:
- Create the `todo_app` database if it doesn't exist
- Create the `todos` table with the required schema
- Set up all necessary columns and indexes

### 4. Table Schema
The `todos` table will be created with the following structure:
```sql
CREATE TABLE todos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL DEFAULT 'MEDIUM',
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## Running the Application
1. Make sure MySQL server is running
2. Run the JavaFX application
3. The database connection will be established automatically
4. If there are connection issues, check the console for error messages

## Troubleshooting

### Connection Issues
- Verify MySQL service is running
- Check username and password in DatabaseConfig.java
- Ensure the MySQL JDBC driver is properly included in dependencies
- Check if the port 3306 is available and not blocked by firewall

### Permission Issues
- Make sure the MySQL user has CREATE DATABASE privileges
- For production, create a dedicated user instead of using root

### Common Errors
- "Access denied": Wrong username/password
- "Connection refused": MySQL service not running
- "Unknown database": Database will be created automatically, but user needs CREATE privileges

## Quick Fix for Your Current Error

**Error**: `Access denied for user 'root'@'localhost' (using password: NO)`

**Solution**: The error indicates that MySQL expects a password for the root user, but the application is trying to connect without one.

### Option 1: Update Application Password (Recommended)
If your MySQL root user has a password, update the `DatabaseConfig.java` file:

```java
private static final String DB_PASSWORD = "your_actual_password"; // Replace with your MySQL root password
```

### Option 2: Reset MySQL Root Password
If you want to use no password (not recommended for production):

1. **Stop MySQL Service**:
   ```bash
   # Windows
   net stop mysql80
   
   # Or use Services.msc to stop MySQL80 service
   ```

2. **Start MySQL in Safe Mode**:
   ```bash
   mysqld --skip-grant-tables --skip-networking
   ```

3. **Connect and Reset Password**:
   ```sql
   mysql -u root
   USE mysql;
   UPDATE user SET authentication_string = '' WHERE user = 'root';
   FLUSH PRIVILEGES;
   EXIT;
   ```

4. **Restart MySQL Service Normally**

### Option 3: Create New Database User
Create a dedicated user for the application:

```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create database and user
CREATE DATABASE todo_app;
CREATE USER 'todoapp'@'localhost' IDENTIFIED BY 'todopass';
GRANT ALL PRIVILEGES ON todo_app.* TO 'todoapp'@'localhost';
FLUSH PRIVILEGES;
```

Then update `DatabaseConfig.java`:
```java
private static final String DB_USER = "todoapp";
private static final String DB_PASSWORD = "todopass";
```

## Production Recommendations
1. Create a dedicated MySQL user for the application
2. Use environment variables for database credentials
3. Enable SSL for database connections
4. Regular database backups
5. Connection pool tuning based on usage
