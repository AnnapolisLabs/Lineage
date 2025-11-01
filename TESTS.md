# Unit Tests

## Test Coverage

### Service Layer Tests (3 files)
- **ProjectServiceTest** - 7 test cases
  - ✅ Create project success
  - ✅ Create project with duplicate key (throws exception)
  - ✅ Get all projects for user
  - ✅ Get project by ID with access
  - ✅ Get project by ID without access (throws exception)
  - ✅ Delete project with admin access
  - ✅ Delete project without admin access (throws exception)

- **RequirementServiceTest** - 7 test cases
  - ✅ Create requirement success
  - ✅ Create requirement as viewer (throws exception)
  - ✅ Get requirements by project
  - ✅ Update requirement success
  - ✅ Delete requirement success
  - ✅ Get requirement history

- **JwtUtilTest** - 5 test cases
  - ✅ Generate token
  - ✅ Extract username from token
  - ✅ Validate token with correct user
  - ✅ Validate token with wrong user
  - ✅ Extract expiration date

### Controller Layer Tests (1 file)
- **AuthControllerTest** - 3 test cases
  - ✅ Login success
  - ✅ Login with invalid request (bad request)
  - ✅ Get current user

### Repository Layer Tests (1 file)
- **UserRepositoryTest** - 5 test cases
  - ✅ Find user by email (exists)
  - ✅ Find user by email (not exists)
  - ✅ Check if user exists by email (true)
  - ✅ Check if user exists by email (false)
  - ✅ Save new user

## Running Tests

### From Command Line
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ProjectServiceTest

# Run tests with coverage
./gradlew test jacocoTestReport
```

### From IDE
- **IntelliJ IDEA**: Right-click on test file or package → Run Tests
- **Eclipse**: Right-click → Run As → JUnit Test

### Test Output
Test results are generated in:
- `build/reports/tests/test/index.html` - HTML report
- `build/test-results/test/` - XML reports

## Test Structure

Tests use:
- **JUnit 5** - Testing framework
- **Mockito** - Mocking dependencies
- **Spring Boot Test** - Integration testing support
- **MockMvc** - Controller testing
- **@DataJpaTest** - Repository testing with in-memory database

## Total Test Count: 27 tests

All tests validate:
- ✅ Happy path scenarios
- ✅ Error handling (exceptions)
- ✅ Access control (permissions)
- ✅ Data validation
- ✅ Security (JWT token handling)
