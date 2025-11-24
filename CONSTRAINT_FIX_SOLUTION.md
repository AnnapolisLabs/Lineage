# Database Constraint Violation Fix - Complete Solution

## Problem Analysis

### User Role Constraint Issue
The error `new row for relation "users" violates check constraint "users_global_role_check"` was caused by a mismatch between:

1. **Database Constraint (V11)**: Only allowed 4 role values: `'ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'VIEWER'`
2. **Java Code Expectations**: The `UserRole` enum had 7 values, but 3 were "legacy" values that should be removed
3. **Default Role**: Java code defaults to `UserRole.USER` but database didn't allow 'USER'

### Team Member Status Constraint Issue
The error `new row for relation "team_members" violates check constraint "team_members_status_check"` was caused by a case mismatch between:

1. **Database Constraint (V14)**: Only allowed lowercase values: `'active', 'inactive', 'pending'`
2. **Java Code Expectations**: The `TeamMemberStatus` enum uses uppercase: `'ACTIVE', 'INACTIVE', 'PENDING'`
3. **Default Status**: Java code defaults to `TeamMemberStatus.ACTIVE` but constraint only allowed lowercase

## Complete Solution - Fresh Start Approach

### 1. Cleaned Up UserRole Enum (UserRole.java)
- **Removed legacy values**: `VIEWER`, `EDITOR`, `ADMIN` (the old confusing ones)
- **Kept clean hierarchy**: `USER`, `PROJECT_MANAGER`, `DEVELOPER`, `OWNER`, `ADMINISTRATOR`
- **Removed legacy mapping function**: No more `mapLegacyRole()` - clean enum

### 2. Updated Application Code
- **Removed legacy imports** from DataLoader.java and PermissionEvaluationService.java
- **Cleaned up User.java** - removed legacy `getRole()` method that called the deleted mapping function
- **Simplified permission logic** - no more legacy role mapping

### 3. Team Member Status Constraint Fix
- **Case mismatch resolved**: Database constraint now accepts uppercase status values to match Java enum
- **Normalized existing data**: All existing status values converted to uppercase for consistency
- **Default status aligned**: Database default set to 'ACTIVE' to match Java code

### 3. Database Migration (V15)
- **Drops restrictive constraint** that only allowed 4 old roles
- **Maps existing data** intelligently to clean roles:
  - Legacy `ADMIN` → `USER` (default)
  - Legacy `VIEWER`, `EDITOR` → `USER`
  - Existing valid roles remain unchanged
- **Creates new constraint** allowing only 5 clean roles: `'USER', 'PROJECT_MANAGER', 'DEVELOPER', 'OWNER', 'ADMINISTRATOR'`

### 4. Team Member Role Migration (V16)
- **Drops old role constraint** that only allowed lowercase role values
- **Normalizes existing data** to uppercase values:
  - `'owner'` → `'OWNER'`
  - `'admin'` → `'ADMIN'` 
  - `'member'` → `'MEMBER'`
  - `'viewer'` → `'VIEWER'`
- **Creates new constraint** allowing uppercase roles: `'OWNER', 'ADMIN', 'MEMBER', 'VIEWER'`

### 5. Team Member Status Migration (V17)
- **Drops old status constraint** that only allowed lowercase status values  
- **Normalizes existing data** to uppercase values:
  - `'active'` → `'ACTIVE'`
  - `'inactive'` → `'INACTIVE'`
  - `'pending'` → `'PENDING'`
- **Creates new constraint** allowing uppercase statuses: `'ACTIVE', 'INACTIVE', 'PENDING'`

### 4. Fixed Compilation Issues
- ✅ **CustomUserDetailsService.java**: Changed `user.getRole()` → `user.getGlobalRole()`
- ✅ **DataLoader.java**: Changed `UserRole.ADMIN` → `UserRole.ADMINISTRATOR`
- ✅ **EnhancedJwtConfig.java**: Removed legacy role cases (`VIEWER`, `EDITOR`, `ADMIN`) from switch statement
- ✅ **Build Status**: All compilation errors resolved, project builds successfully

### 5. Files Modified

#### Java Files:
- ✅ `src/main/java/com/annapolislabs/lineage/entity/UserRole.java` - Clean enum, removed legacy values
- ✅ `src/main/java/com/annapolislabs/lineage/entity/User.java` - Removed legacy methods
- ✅ `src/main/java/com/annapolislabs/lineage/config/DataLoader.java` - Updated role references
- ✅ `src/main/java/com/annapolislabs/lineage/service/PermissionEvaluationService.java` - Cleaned up permission logic
- ✅ `src/main/java/com/annapolislabs/lineage/security/CustomUserDetailsService.java` - Fixed method call
- ✅ `src/main/java/com/annapolislabs/lineage/config/EnhancedJwtConfig.java` - Removed legacy role cases

#### Database Files:
- ✅ `src/main/resources/db/migration/V15__Fix_user_role_constraint.sql` - User role constraint fix
- ✅ `src/main/resources/db/migration/V16__Fix_team_members_role_constraint.sql` - Team member role constraint fix
- ✅ `src/main/resources/db/migration/V17__Fix_team_members_status_constraint.sql` - Team member status constraint fix

#### Documentation:
- ✅ `CONSTRAINT_FIX_SOLUTION.md` - This solution document

## New Clean Role Hierarchy

```
OWNER (Level 3)          - Super user, full system access
ADMINISTRATOR (Level 2)  - Admin, system configuration and user management
PROJECT_MANAGER (Level 2) - Project oversight and team management  
DEVELOPER (Level 1)      - Developer, can create/edit requirements
USER (Level 1)           - Standard user, read-only access
```

## Usage

1. **Apply the V15 migration** when the application starts - fixes user global_role constraint
2. **Apply the V16 migration** when the application starts - fixes team_members role constraint  
3. **Apply the V17 migration** when the application starts - fixes team_members status constraint
4. **Restart the application** to pick up all enum changes
5. **Test team member operations** - should work without constraint violations

## Result

### User Role Issues
- ❌ **Before**: User creation failed with constraint violation, confusing legacy roles
- ✅ **After**: Clean 5-role hierarchy, user creation succeeds, no legacy code

### Team Member Issues  
- ❌ **Before**: Team member creation failed with status constraint violation (case mismatch)
- ✅ **After**: Status and role constraints aligned with Java enums, consistent uppercase values
- ✅ **Safe**: Data migration with backup, comprehensive verification
- ✅ **Clean**: Removed all case sensitivity issues, consistent enum mapping
- ✅ **Maintainable**: Clear role/status hierarchy, no confusing mappings between Java and database