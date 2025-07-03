# Database Configuration Guide

## Overview
This application uses a secure, environment-aware database configuration system that follows enterprise software architecture best practices.

## Configuration Files

### 1. `database.properties.template`
Template file showing all available configuration options. Copy this to create your environment-specific configuration.

### 2. `database.properties` (Git ignored)
Your local development database configuration. This file is automatically ignored by Git to prevent credentials from being committed.

### 3. `database-local.properties` (Git ignored)
Optional local override configuration with highest priority.

## Configuration Priority (Highest to Lowest)
1. **System Properties** (`-Ddb.url=...`)
2. **Environment Variables** (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
3. **database-local.properties**
4. **database.properties**
5. **database-default.properties**

## Setup Instructions

### For Development
1. Copy `database.properties.template` to `database.properties`
2. Update the credentials in `database.properties` with your local MySQL settings
3. The file will be automatically ignored by Git

### For Production
Use environment variables or system properties:
```bash
export DB_URL="jdbc:mysql://prod-server:3306/todo_app"
export DB_USERNAME="prod_user"
export DB_PASSWORD="secure_password"
```

Or with system properties:
```bash
java -Ddb.url="jdbc:mysql://prod-server:3306/todo_app" \
     -Ddb.username="prod_user" \
     -Ddb.password="secure_password" \
     -jar todo-app.jar
```

## Available Configuration Properties

### Database Connection
- `db.url` - JDBC connection URL
- `db.username` - Database username
- `db.password` - Database password

### Connection Pool (HikariCP)
- `db.pool.maximumPoolSize` - Maximum number of connections (default: 10)
- `db.pool.minimumIdle` - Minimum idle connections (default: 2)
- `db.pool.connectionTimeout` - Connection timeout in ms (default: 30000)
- `db.pool.idleTimeout` - Idle timeout in ms (default: 600000)
- `db.pool.maxLifetime` - Max connection lifetime in ms (default: 1800000)

### Schema Management
- `db.schema.name` - Database schema name (default: todo_app)
- `db.schema.autoCreate` - Auto-create database tables (default: true)

## Security Best Practices

✅ **DO:**
- Use environment variables for production
- Use separate configuration files per environment
- Keep `database.properties.template` in version control
- Regularly rotate database passwords

❌ **DON'T:**
- Commit actual database credentials to version control
- Use the same passwords across environments
- Hard-code credentials in source code
- Share production credentials via insecure channels

## Troubleshooting

### Common Issues
1. **Missing configuration**: Check that `database.properties` exists and contains required properties
2. **Connection failures**: Verify MySQL is running and credentials are correct
3. **Permission errors**: Ensure database user has necessary permissions

### Logging
The application logs database configuration loading and connection status. Check logs for configuration-related issues.
