# Running Tests

## Prerequisites

Ensure Java 21 is installed and JAVA_HOME is set.

## Run All Tests

### Windows
```bash
gradlew.bat test
```

### Linux/Mac
```bash
./gradlew test
```

## Run Specific Test Class

```bash
# Windows
gradlew.bat test --tests ProjectServiceTest

# Linux/Mac
./gradlew test --tests ProjectServiceTest
```

## Run Tests with Coverage

```bash
# Windows
gradlew.bat test jacocoTestReport

# Linux/Mac
./gradlew test jacocoTestReport
```

Coverage report will be at: `build/reports/jacoco/test/html/index.html`

## View Test Results

After running tests, open:
```
build/reports/tests/test/index.html
```

## Test Structure

### Service Tests
- **ProjectServiceTest** - 7 tests
  - Create project success
  - Create project with duplicate key
  - Get all projects
  - Get project by ID with/without access
  - Delete project with/without admin access

- **RequirementServiceTest** - 7 tests
  - Create requirement success
  - Create requirement as viewer (permission check)
  - Get requirements by project
  - Update requirement
  - Delete requirement
  - Get requirement history

- **JwtUtilTest** - 5 tests
  - Generate token
  - Extract username
  - Validate token (valid/invalid user)
  - Extract expiration

### Controller Tests
- **AuthControllerTest** - 3 tests
  - Login success
  - Login with invalid request
  - Get current user

### Repository Tests
- **UserRepositoryTest** - 5 tests
  - Find user by email
  - Check user exists
  - Save new user

## Expected Output

When all tests pass, you should see:
```
BUILD SUCCESSFUL in Xs
27 tests completed, 27 succeeded
```

## Common Issues

### Java Not Found
```
ERROR: JAVA_HOME is not set
```
**Solution:** Install Java 21 and set JAVA_HOME environment variable

### Database Connection Error
Tests use H2 in-memory database by default, no PostgreSQL needed for tests.

### Port Already in Use
Unit tests don't start the server, so port conflicts shouldn't occur.

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: ./gradlew test
      - name: Upload test results
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: build/reports/tests/test/
```

## Test Coverage Goal

Current: 27 tests covering critical paths
Goal: 80%+ code coverage

Areas with tests:
- ✅ Service layer business logic
- ✅ Security (JWT)
- ✅ Controllers (Auth)
- ✅ Repositories (User queries)

Areas to add tests:
- [ ] RequirementLinkService
- [ ] ExportService
- [ ] SearchController
- [ ] All repository methods
