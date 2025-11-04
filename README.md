# Hotel Reservation System

A Spring Boot application for managing hotel reservations, built with Java, PostgreSQL, and Redis.

---

## Table of Contents

- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Development Guidelines](#development-guidelines)
- [Testing Guidelines](#testing-guidelines)
- [Git Workflow](#git-workflow)

---

## Getting Started

### Prerequisites

- Java 25
- PostgreSQL
- Redis
- Gradle

### Running the Application

```bash
./gradlew bootRun
```

---

## Project Structure

```
src/main/java/com/example/hotelreservationsystem/
├── config/          # Configuration classes (JPA, Security, etc.)
├── controllers/     # REST API endpoints
├── converter/       # JPA AttributeConverters
├── entity/          # JPA entities
├── enums/           # Enum types
├── repository/      # Spring Data JPA repositories
└── service/         # Business logic layer
```

---

## Coding Standards

### 1. Lombok Usage

#### ✅ DO: Use Explicit Annotations for JPA Entities

**For entities with inheritance:**
```java
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"circularReferences"})
@EqualsAndHashCode(callSuper = true, exclude = {"circularReferences"})
@Entity
@Table(name = "entity_name")
public class MyEntity extends BaseEntityAudit {
    // fields
}
```

**Rationale:**
- `@Data` includes `@EqualsAndHashCode(callSuper = false)` by default, which is incorrect for entities with inheritance
- Explicit annotations give full control over `callSuper` and `exclude` parameters
- Bidirectional relationships MUST be excluded to prevent circular references

#### ❌ DON'T: Use @Data with Inheritance

```java
// WRONG - causes callSuper issues
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class MyEntity extends BaseEntity {
    // ...
}
```

#### Circular Reference Prevention Checklist

For bidirectional relationships (e.g., `Hotel ↔ Room`):

1. **Lombok:** Exclude from `@ToString` and `@EqualsAndHashCode`
2. **Jackson:** Use `@JsonManagedReference` (parent) and `@JsonBackReference` (child)

**Example:**
```java
// Parent entity (Hotel)
@OneToMany(mappedBy = "hotel")
@JsonManagedReference
private List<Room> rooms;

// Child entity (Room)
@ManyToOne
@JoinColumn(name = "hotel_id")
@JsonBackReference
private Hotel hotel;
```

---

### 2. JPA Entity Best Practices

#### Entity Base Classes

**All entities must extend either:**
- `BaseEntity` - For entities without audit fields
- `BaseEntityAudit` - For entities requiring created/updated tracking

#### Required Annotations

```java
@Getter
@Setter
@NoArgsConstructor              // Required by JPA
@ToString(callSuper = true)     // Include parent fields
@EqualsAndHashCode(callSuper = true)  // Include ID from parent
@Entity
@Table(name = "table_name")     // Always specify table name
public class MyEntity extends BaseEntityAudit {
    // ...
}
```

#### Relationship Annotations

**@ManyToOne / @OneToOne:**
```java
@ManyToOne(fetch = FetchType.LAZY)  // Always use LAZY
@JoinColumn(name = "foreign_key_id")
private RelatedEntity relatedEntity;
```

**@OneToMany:**
```java
@OneToMany(mappedBy = "fieldName", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ChildEntity> children = new ArrayList<>();  // Initialize collections
```

**❌ DON'T: Add @Column to relationship fields**
```java
// WRONG
@Column  // Remove this!
@OneToMany(mappedBy = "hotel")
private List<Room> rooms;
```

#### Enum Handling

**Use `@Enumerated(EnumType.STRING)` for all enums:**

```java
@Column(name = "status")
@Enumerated(EnumType.STRING)  // Store as VARCHAR, not ordinal
private RoomStatus status;
```

**Rationale:**
- Resilient to enum reordering
- More readable in database
- Easier debugging

**❌ DON'T: Use AttributeConverter for enums unless absolutely necessary**
```java
// Avoid unless you have specific requirements
@Convert(converter = MyEnumConverter.class)
private MyEnum myEnum;
```

#### Database Indexes

**Add indexes for commonly queried fields:**

```java
@Entity
@Table(
    name = "room",
    indexes = {
        @Index(name = "idx_room_status", columnList = "room_status"),
        @Index(name = "idx_room_type_status", columnList = "room_type_id, room_status"),
        @Index(name = "idx_hotel_id", columnList = "hotel_id")
    }
)
public class Room extends BaseEntityAudit {
    // ...
}
```

**Rules:**
- Index foreign keys
- Index frequently filtered columns
- Use composite indexes for multi-column queries
- Name indexes: `idx_{table}_{column}` or `idx_{columns}`

---

### 3. Naming Conventions

#### Java Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Class | PascalCase | `CustomerService`, `RoomRepository` |
| Method | camelCase | `findById()`, `searchAvailableRooms()` |
| Variable | camelCase | `roomTypeId`, `customerName` |
| Constant | UPPER_SNAKE_CASE | `MAX_CAPACITY`, `DEFAULT_TIMEOUT` |
| Package | lowercase | `com.example.hotelreservationsystem` |

#### Database Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Table | snake_case | `customer`, `room_type` |
| Column | snake_case | `created_at`, `room_type_id` |
| Foreign Key | `{table}_id` | `hotel_id`, `customer_id` |
| Index | `idx_{table}_{column}` | `idx_room_status` |

#### Field Naming Rules

```java
// ✅ Correct
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;  // lowercase

@Column(name = "created_at")
private LocalDateTime createdAt;  // camelCase in Java

// ❌ Wrong
private Long Id;  // uppercase first letter
private LocalDateTime created_at;  // snake_case in Java
```

---

### 4. Repository Layer

#### Repository Naming

```java
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Query methods
}
```

#### Query Method Naming

**Use Spring Data JPA naming conventions:**

```java
// Simple queries
List<Customer> findByEmail(String email);
List<Customer> findByMembershipTier(MembershipTier tier);

// Property expressions (nested properties)
List<Room> findByRoomType_IdAndRoomStatus(Long roomTypeId, RoomStatus status);

// Multiple conditions
List<Customer> findByNameAndEmail(String name, String email);

// Ordering
List<Room> findByRoomStatusOrderByRoomNumberAsc(RoomStatus status);
```

#### Prevent N+1 Queries

**Option 1: @EntityGraph (recommended)**
```java
@EntityGraph(attributePaths = {"hotel", "roomType"})
List<Room> findByRoomStatus(RoomStatus status);
```

**Option 2: @Query with JOIN FETCH**
```java
@Query("SELECT r FROM Room r JOIN FETCH r.hotel JOIN FETCH r.roomType WHERE r.roomStatus = :status")
List<Room> findAvailableRoomsWithDetails(@Param("status") RoomStatus status);
```

---

### 5. Service Layer

#### Service Class Structure

```java
@Service
@RequiredArgsConstructor  // or @AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    // Business logic methods
    public Customer createCustomer(Customer customer) {
        // Validation
        // Business logic
        return customerRepository.save(customer);
    }
}
```

#### Dependency Injection

**✅ DO: Use constructor injection**
```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
}
```

**❌ DON'T: Use field injection**
```java
// Avoid this
@Service
public class RoomService {
    @Autowired
    private RoomRepository roomRepository;  // Harder to test
}
```

---

### 6. Controller Layer

#### REST Controller Structure

```java
@RestController
@RequestMapping("/api/v1/customers")  // Versioned API
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody @Valid Customer customer) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(customerService.createCustomer(customer));
    }
}
```

#### Parameter Naming

**Use descriptive names matching the domain:**
```java
// ✅ Good - clear and descriptive
@GetMapping("/search")
public List<Room> searchRooms(@RequestParam Long roomTypeId) {
    // ...
}

// ❌ Bad - ambiguous or misleading
@GetMapping("/search")
public List<Room> searchRooms(@RequestParam long roomType) {  // What is roomType?
    // ...
}
```

#### Error Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

**❌ DON'T: Swallow exceptions**
```java
// Bad practice
try {
    return roomService.findAll();
} catch (Exception e) {
    System.console().printf(e.getMessage());  // console() is often null!
    return List.of();  // Hides the error from the user
}
```

---

### 7. Configuration Classes

#### JPA Auditing Configuration

All audit fields are automatically populated via `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`.

**Location:** `src/main/java/com/example/hotelreservationsystem/config/JpaAuditingConfig.java`

**When implementing Spring Security, update:**
```java
@Bean
public AuditorAware<String> auditorProvider() {
    return () -> {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }
        return Optional.of(authentication.getName());
    };
}
```

---

### 8. Exception Handling

#### Custom Exceptions

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

#### Service Layer Exception Handling

```java
public Customer findById(Long id) {
    return customerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
}
```

---

### 9. AttributeConverter Best Practices

#### When to Use Converters

Use AttributeConverter when:
- You need custom database storage format
- You need validation during conversion
- You need backward compatibility with existing data

#### Converter Implementation

```java
@Converter(autoApply = true)  // or false if not globally applied
public class RoomStatusConverter implements AttributeConverter<RoomStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RoomStatus status) {
        if (status == null) {
            return null;
        }
        return status.ordinal();
    }

    @Override
    public RoomStatus convertToEntityAttribute(Integer value) {
        if (value == null) {
            return null;
        }

        RoomStatus[] statuses = RoomStatus.values();
        if (value < 0 || value >= statuses.length) {
            throw new IllegalArgumentException(
                "Unknown status value: " + value + ". Valid range: 0-" + (statuses.length - 1)
            );
        }

        return statuses[value];
    }
}
```

**Always include:**
1. Null checks
2. Bounds validation
3. Descriptive error messages

---

## Development Guidelines

### 1. Before Starting Work

1. Pull latest changes: `git pull origin main`
2. Create feature branch: `git checkout -b feature/your-feature-name`
3. Review related entity classes and relationships

### 2. Making Changes

#### Entity Changes Checklist

- [ ] Added/updated entity annotations correctly
- [ ] Excluded bidirectional relationships from `@ToString`/`@EqualsAndHashCode`
- [ ] Used `@Enumerated(EnumType.STRING)` for enums
- [ ] Added database indexes for new query fields
- [ ] Updated repository query methods if needed
- [ ] Tested lazy loading doesn't cause N+1 queries

#### Repository Changes Checklist

- [ ] Query method names follow Spring Data conventions
- [ ] Used `@EntityGraph` or JOIN FETCH for relationships
- [ ] Added `@Param` annotations for custom queries
- [ ] Tested query returns expected results

#### Service Changes Checklist

- [ ] Used constructor injection (`@RequiredArgsConstructor`)
- [ ] Added proper exception handling
- [ ] Implemented business logic validation
- [ ] Documented complex business rules

#### Controller Changes Checklist

- [ ] Used versioned API paths (`/api/v1/...`)
- [ ] Parameter names are descriptive
- [ ] Used `@Valid` for request bodies
- [ ] Returns proper HTTP status codes
- [ ] Added error handling

### 3. Code Review Checklist

Before submitting PR, verify:

- [ ] No compilation errors
- [ ] All tests pass
- [ ] No `@Data` annotation with inheritance
- [ ] Circular references prevented
- [ ] Lombok annotations are explicit
- [ ] Enum handling is consistent (`@Enumerated(STRING)`)
- [ ] Database indexes added for new queries
- [ ] No field injection (`@Autowired` on fields)
- [ ] Proper exception handling (no swallowed exceptions)
- [ ] Code follows naming conventions

---

## Testing Guidelines

### Unit Tests

```java
@SpringBootTest
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @MockBean
    private CustomerRepository customerRepository;

    @Test
    void shouldCreateCustomer() {
        // Given
        Customer customer = new Customer();
        customer.setName("John Doe");

        when(customerRepository.save(any(Customer.class)))
            .thenReturn(customer);

        // When
        Customer result = customerService.createCustomer(customer);

        // Then
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(customerRepository).save(customer);
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureTestDatabase
class JpaAuditingTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldPopulateAuditFields() {
        Customer customer = new Customer();
        customer.setName("Test User");

        Customer saved = customerRepository.save(customer);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("SYSTEM");
    }
}
```

---

## Git Workflow

### Branch Naming

- Feature: `feature/add-room-booking`
- Bugfix: `bugfix/fix-date-validation`
- Hotfix: `hotfix/critical-security-issue`
- Refactor: `refactor/optimize-queries`

### Commit Messages

Follow the Conventional Commits specification:

```
type(scope): subject

body (optional)

footer (optional)
```

**Types:**
- `feat:` New feature
- `fix:` Bug fix
- `refactor:` Code refactoring
- `docs:` Documentation
- `test:` Adding tests
- `chore:` Maintenance

**Examples:**
```
feat(customer): add email validation

fix(room): prevent duplicate room numbers

refactor(entity): replace @Data with explicit Lombok annotations

- Fixed circular reference issues
- Added @NoArgsConstructor for JPA compatibility
- Excluded bidirectional relationships from toString/equals
```

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Refactoring
- [ ] Documentation

## Checklist
- [ ] Code follows project standards
- [ ] Tests added/updated
- [ ] No circular reference issues
- [ ] Proper Lombok annotations used
- [ ] Database indexes added if needed

## Testing
Describe how you tested the changes
```

---

## Common Pitfalls to Avoid

### 1. Circular Reference Hell

**Problem:** StackOverflowError when calling `toString()` or serializing to JSON

**Solution:**
- Exclude bidirectional relationships from `@ToString` and `@EqualsAndHashCode`
- Use `@JsonManagedReference`/`@JsonBackReference` for JSON serialization

### 2. N+1 Query Problem

**Problem:** Fetching 100 rooms triggers 100+ additional queries for related entities

**Solution:**
- Use `@EntityGraph` or JOIN FETCH in repository methods
- Always test with profiling enabled

### 3. LazyInitializationException

**Problem:** Accessing lazy-loaded field outside transaction

**Solution:**
- Use `@Transactional` on service methods
- Fetch relationships explicitly with JOIN FETCH
- Return DTOs instead of entities from controllers

### 4. @Data with Inheritance

**Problem:** `equals()` and `hashCode()` don't include parent class fields

**Solution:**
- Never use `@Data` with JPA entities that extend other entities
- Use explicit Lombok annotations with `callSuper = true`

### 5. Database Destruction

**Problem:** `spring.jpa.hibernate.ddl-auto=create` deletes all data on restart

**Solution:**
- Use `validate` for production
- Use `update` for development
- NEVER commit `create` or `create-drop` settings

---

## Additional Resources

- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [Lombok Documentation](https://projectlombok.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Java Naming Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-namingconventions.html)

---

## Contributing

1. Read this README thoroughly
2. Follow all coding standards
3. Write tests for new features
4. Submit PR with clear description
5. Address code review feedback

---

## Support

For questions or issues, contact the team lead or open an issue in the repository.
