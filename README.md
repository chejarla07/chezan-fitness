# Zuora Customer Creator UI

A Spring Boot web application with a modern UI for creating customer accounts in Zuora. The form is styled like e-commerce websites with pre-filled default values.

## Features

- Modern, responsive UI with Bootstrap 5 and Font Awesome icons
- Pre-filled default values for quick testing
- Form validation (required fields)
- Integration with Zuora REST API
- SQLite database for storing API responses
- RESTful backend with Thymeleaf templates

## Prerequisites

- Java 17 or higher
- Apache Maven
- Internet connection (for Bootstrap CDN and Zuora API)

## Getting Started

### 1. Clone and navigate to project directory

```bash
cd zuora-customer-creator
```

### 2. Build the project

```bash
mvn clean package
```

### 3. Run the application

```bash
java -jar target/zuora-customer-creator-1.0-SNAPSHOT.jar
```

Or using Maven:

```bash
mvn spring-boot:run
```

### 4. Access the application

Open your browser and navigate to: http://localhost:8080

## Configuration

### Zuora API Credentials

Edit `src/main/java/com/example/zuora/ZuoraService.java` to update:

```java
private static final String ZUORA_BASE_URL = "https://rest.apisandbox.zuora.com";
private static final String CLIENT_ID = "your-client-id";
private static final String CLIENT_SECRET = "your-client-secret";
```

### Database

The application uses SQLite with a local file `zuora_db.sqlite3` in the project root. The table `zuora_api_responses` is automatically created on startup.

## Project Structure

```
src/main/java/com/example/zuora/
├── Application.java              # Spring Boot entry point
├── CustomerController.java       # Web controller
├── CustomerForm.java             # Form data model
├── ZuoraService.java             # Zuora API integration
└── DatabaseInitializer.java      # Database setup

src/main/resources/
├── templates/
│   ├── customer-form.html       # Main form UI
│   └── result.html              # Submission result page
└── application.properties        # Spring configuration
```

## UI Features

- **Modern e-commerce design**: Gradient headers, card layouts, subtle shadows
- **Responsive layout**: Works on mobile and desktop
- **Form validation**: Required fields marked with asterisks
- **Default values**: Pre-filled for quick testing
- **Reset button**: Restores default values
- **Success/error pages**: Clear feedback after submission

## API Integration

The application integrates with Zuora's REST API:

1. **Authentication**: OAuth2 client credentials flow
2. **Account creation**: POST to `/v1/accounts`
3. **Response storage**: All API responses stored in SQLite database

## Testing

1. Start the application
2. Navigate to http://localhost:8080
3. Verify default values are pre-filled
4. Click "Create Customer Account"
5. Check the result page for success/error

## Notes

- The default credentials are for Zuora sandbox environment
- Replace with your own credentials for production use
- The SQLite database file is created automatically
- All API responses are logged to the database for audit purposes

## Troubleshooting

**Application fails to start:**
- Check Java version (Java 17+ required)
- Ensure port 8080 is available

**Zuora API errors:**
- Verify credentials in ZuoraService.java
- Check network connectivity to Zuora API

**Database errors:**
- Ensure write permissions in project directory
- Check SQLite JDBC driver compatibility

## License

This project is for demonstration purposes only.