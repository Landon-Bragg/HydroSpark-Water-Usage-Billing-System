# HydroSpark Water Usage & Billing System

Complete water utility billing system with customer portal, staff management, and automated billing.

## 🎯 What This System Does

**For Customers:**
- View daily and monthly water usage charts
- See estimated next bill amount
- Access current and historical bills
- Get alerts for unusual usage

**For HydroSpark Staff:**
- Import water usage data (supports 2M+ rows)
- Configure tiered/seasonal pricing
- Generate and send bills automatically
- Detect usage anomalies (spikes, leaks, etc.)
- Role-based access control

## 📋 Prerequisites

You'll need to install these (one-time setup):

1. **Java 21** - Download from https://adoptium.net/
2. **Node.js 20+** - Download from https://nodejs.org/
3. **Docker Desktop** - Download from https://www.docker.com/products/docker-desktop/
4. **Git** (optional but recommended) - Download from https://git-scm.com/

### Verify installations:
```bash
java -version    # Should show version 21.x
node -version    # Should show v20.x or higher
docker --version # Should show Docker version 20.x+
```

## 🚀 Quick Start Guide (Step-by-Step)

### Step 1: Start the Database

1. Open a terminal/command prompt
2. Navigate to this project folder:
   ```bash
   cd path/to/hydrospark-system
   ```

3. Start MySQL with Docker:
   ```bash
   docker-compose up -d
   ```
   
   This will:
   - Download MySQL (first time only)
   - Start MySQL on port 3306
   - Create database 'hydrospark'

4. Wait 30 seconds for MySQL to fully start

### Step 2: Import Your 2 Million Rows

Place your Excel file in the `backend/src/main/resources/data/` folder and rename it to `water_usage_data.xlsx`

OR use the import script:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments="--import.file=/path/to/your/file.xlsx"
```

The import will:
- Process ~10,000 rows per second
- Show progress every 50,000 rows
- Complete in about 3-5 minutes for 2M rows
- Create customers, meters, and daily readings

### Step 3: Start the Backend

```bash
cd backend
./mvnw spring-boot:run
```

First run will take 2-3 minutes to download dependencies.

Backend will start at: http://localhost:8080

You'll see: "Application started successfully"

### Step 4: Start the Frontend

Open a NEW terminal window:

```bash
cd frontend
npm install  # First time only - takes 1-2 minutes
npm start
```

Frontend will open automatically at: http://localhost:3000

## 🔑 Default Login Credentials

### Admin User
- Email: admin@hydrospark.com
- Password: Admin123!

### Customer User
- Email: customer@hydrospark.com
- Password: Customer123!

### Billing Staff
- Email: billing@hydrospark.com
- Password: Billing123!

### Operations Staff
- Email: operations@hydrospark.com
- Password: Operations123!

## 📊 Running Your First Billing Cycle

1. Log in as admin@hydrospark.com
2. Go to **Rate Plans** → Verify "Standard Residential" plan is ACTIVE
3. Go to **Billing** → Click "Generate Billing Periods"
4. Select month/year → Click Generate
5. Click "Run Billing" for the generated period
6. Review bills → Click "Issue Bills"
7. Bills are now visible to customers in their portal

## 🗄️ Database Access

If you want to view the database directly:

```bash
docker exec -it hydrospark-mysql mysql -u hydrospark -phydrospark123 hydrospark
```

Useful queries:
```sql
-- Count total readings
SELECT COUNT(*) FROM meter_readings;

-- View customers
SELECT * FROM customers LIMIT 10;

-- View latest bills
SELECT * FROM bills ORDER BY created_at DESC LIMIT 10;

-- Check anomalies
SELECT * FROM anomaly_events WHERE status = 'OPEN';
```

Exit with: `exit`

## 📁 Project Structure

```
hydrospark-system/
├── backend/                 # Java Spring Boot API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/         # Application code
│   │   │   └── resources/
│   │   │       ├── db/migration/  # Database schemas
│   │   │       └── data/          # Put your Excel file here
│   │   └── test/            # Tests
│   └── pom.xml              # Dependencies
├── frontend/                # React TypeScript UI
│   ├── src/
│   │   ├── components/      # UI components
│   │   ├── pages/           # Application pages
│   │   └── services/        # API calls
│   └── package.json         # Dependencies
├── docker-compose.yml       # Database setup
└── README.md               # This file
```

## 🔧 Common Issues & Solutions

### "Port 3306 already in use"
MySQL might already be running on your computer.
```bash
# Stop existing MySQL
sudo service mysql stop  # Linux
# or
net stop MySQL  # Windows (as Administrator)
```

### "Cannot connect to database"
Wait 30 seconds after `docker-compose up -d` for MySQL to fully start.

Check if it's running:
```bash
docker ps  # Should show hydrospark-mysql
```

### "npm install fails"
Try clearing the cache:
```bash
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### "Java version wrong"
Make sure JAVA_HOME points to Java 21:
```bash
# Windows
set JAVA_HOME=C:\Path\To\Java21

# Mac/Linux
export JAVA_HOME=/path/to/java21
```

## 📚 Features Overview

### Customer Portal
- **Dashboard**: Current usage, estimated next bill, alerts
- **Usage History**: Interactive daily/monthly charts
- **Bills**: View and download all bills
- **Profile**: Update contact info and preferences

### Staff Application

#### Admin
- Full system access
- User management
- System configuration

#### Billing
- Configure rate plans (tiered, seasonal, surcharges)
- Generate billing periods
- Run billing cycles
- Issue and send bills

#### Operations
- View usage anomalies
- Investigate spikes/leaks
- Monitor data quality
- Flag issues for customer outreach

#### Support
- View customer accounts
- Access bill history
- Create support notes
- Resend portal invitations

## 🤖 Automated Features

### Anomaly Detection (runs nightly at 2 AM)
- **Spike**: Usage > mean + 3σ
- **Sustained High**: 3+ days at 2× normal usage
- **Zero Usage**: 7+ consecutive days of zero
- **Data Gaps**: Missing 3+ consecutive days

### Scheduled Jobs
- **Nightly**: Anomaly scan, data validation (2:00 AM)
- **Monthly**: Auto-generate billing periods (1st, 12:10 AM)
- **Weekly**: Forecast generation for upcoming bills

## 🧪 Testing

### Run Backend Tests
```bash
cd backend
./mvnw test
```

Tests include:
- Rate engine calculations (tiered pricing)
- Billing accuracy
- Security (RBAC enforcement)
- Anomaly detection logic

### Run Frontend Tests
```bash
cd frontend
npm test
```

## 📈 Performance

System handles:
- **50,000+ customers**
- **2M+ daily readings**
- **Billing run**: 10,000 customers in < 5 minutes
- **Import speed**: ~10,000 rows/second

## 🔒 Security

- JWT-based authentication (15 min access, 7 day refresh)
- BCrypt password hashing
- Role-based access control (RBAC)
- Account lockout (10 failed attempts / 15 min)
- Audit logging for all sensitive operations

## 📖 API Documentation

Once backend is running, visit:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

## 🛠️ Development

### Backend Development
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Hot reload is enabled - changes to Java files rebuild automatically.

### Frontend Development
```bash
cd frontend
npm start
```

Changes appear immediately (hot reload enabled).

### Database Migrations

Migrations run automatically on startup. Located in:
`backend/src/main/resources/db/migration/`

To create a new migration:
1. Create file: `V{next_version}__description.sql`
2. Add SQL statements
3. Restart backend

## 📞 Support

For issues or questions:
1. Check "Common Issues" section above
2. Review logs: `docker logs hydrospark-mysql`
3. Backend logs appear in terminal where you ran `./mvnw spring-boot:run`

## 🎨 Customization

### Brand Colors
Edit `frontend/src/styles/theme.ts` to match HydroSpark branding.

### Rate Plans
Configure in UI: Admin → Rate Plans → Create New

Or via SQL:
```sql
-- Example: Add peak season multiplier
INSERT INTO rate_components (id, rate_plan_id, component_type, name, config_json, sort_order)
VALUES (UUID(), 'rate_plan_id', 'SEASONAL_MULTIPLIER', 'Summer Peak', 
        '{"applies_months": [6,7,8], "multiplier": 1.25}', 3);
```

## 🚢 Deployment (Production)

See `DEPLOYMENT.md` for production deployment instructions including:
- Cloud hosting (AWS, Azure, GCP)
- SSL certificates
- Email service integration (SendGrid)
- Backup procedures
- Monitoring setup

## 📄 License

Copyright © 2026 HydroSpark. All rights reserved.

---

**Built with**: Java 21, Spring Boot 3.2, React 18, TypeScript, MySQL 8, Docker
