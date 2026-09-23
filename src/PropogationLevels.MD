# Spring `@Transactional` Propagation Levels

---

## What is Propagation?

Propagation defines **how a transactional method behaves when it is called from within or outside an existing transaction**.

In simple terms:

> Propagation answers the question: *“If a transaction already exists, should this method join it, create a new one, or run without a transaction?”*

Spring provides 7 propagation types via the `Propagation` enum.

---

## The 7 Propagation Types

### REQUIRED (default)

- **Behavior:** Joins the existing transaction if one exists. If no transaction exists, creates a new one.
- **Use case:** Most common. Use when you want the method to always run inside a transaction.
- **Rollback:** If the outer transaction rolls back, the inner method’s work is also rolled back.

```java
@Transactional(propagation = Propagation.REQUIRED)
public void doWork() { ... }
```

---

### REQUIRES_NEW

- **Behavior:** Always creates a **new** transaction. If an existing transaction exists, it is **suspended**.
- **Use case:** When you want the inner method to commit or roll back independently of the outer transaction.
- **Rollback:** The inner transaction can commit even if the outer transaction later rolls back.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void auditLog() { ... }
```

---

### SUPPORTS

- **Behavior:** Joins the existing transaction if one exists. If no transaction exists, runs **without** a transaction.
- **Use case:** For methods that can run with or without a transaction (e.g., read-only queries).
- **Rollback:** No transaction means no rollback management by Spring.

```java
@Transactional(propagation = Propagation.SUPPORTS)
public void readData() { ... }
```

---

### NOT_SUPPORTED

- **Behavior:** Always runs **without** a transaction. If an existing transaction exists, it is **suspended**.
- **Use case:** For operations that should not run inside a transaction (e.g., long-running batch jobs).
- **Rollback:** No transaction means no rollback.

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void longRunningJob() { ... }
```

---

### MANDATORY

- **Behavior:** Requires an existing transaction. If no transaction exists, throws `IllegalTransactionStateException`.
- **Use case:** When a method must be called only from within an existing transaction.
- **Rollback:** Participates in the existing transaction.

```java
@Transactional(propagation = Propagation.MANDATORY)
public void mustBeInTransaction() { ... }
```

---

### NEVER

- **Behavior:** Must **not** run inside a transaction. If a transaction exists, throws `IllegalTransactionStateException`.
- **Use case:** When a method should never be called within a transaction.
- **Rollback:** No transaction means no rollback.

```java
@Transactional(propagation = Propagation.NEVER)
public void noTransactionAllowed() { ... }
```

---

### NESTED

- **Behavior:** Executes within a **nested transaction** using JDBC savepoints. If no transaction exists, behaves like `REQUIRED`.
- **Use case:** When you want partial rollback — inner transaction can roll back to a savepoint without affecting the outer transaction.
- **Rollback:** Inner rollback only rolls back to the savepoint, not the entire outer transaction.
- **Note:** Requires the underlying database to support savepoints. Works with JDBC and Hibernate (JPA) if savepoints are supported.

```java
@Transactional(propagation = Propagation.NESTED)
public void nestedOperation() { ... }
```

---

## Comparison Table

| Propagation | Existing Transaction? | Behavior | Rollback Effect |
|---|---|---|---|
| **REQUIRED** | Yes | Joins existing | Outer rollback rolls back inner |
| **REQUIRED** | No | Creates new | Normal rollback |
| **REQUIRES_NEW** | Yes | Suspends existing, creates new | Inner rollback independent |
| **REQUIRES_NEW** | No | Creates new | Normal rollback |
| **SUPPORTS** | Yes | Joins existing | Outer rollback rolls back inner |
| **SUPPORTS** | No | Runs without transaction | No rollback management |
| **NOT_SUPPORTED** | Yes | Suspends existing, runs without | No rollback management |
| **NOT_SUPPORTED** | No | Runs without transaction | No rollback management |
| **MANDATORY** | Yes | Joins existing | Outer rollback rolls back inner |
| **MANDATORY** | No | Throws exception | — |
| **NEVER** | Yes | Throws exception | — |
| **NEVER** | No | Runs without transaction | No rollback management |
| **NESTED** | Yes | Creates savepoint | Inner rollback to savepoint |
| **NESTED** | No | Creates new (like REQUIRED) | Normal rollback |

---

## Code Examples

### REQUIRED Example

```java
@Service
public class OrderService {

    @Autowired
    private PaymentService paymentService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void placeOrder() {
        saveOrder();
        paymentService.processPayment(); // joins same transaction
        // if processPayment throws, both roll back
    }
}

@Service
public class PaymentService {

    @Transactional(propagation = Propagation.REQUIRED)
    public void processPayment() {
        // same transaction as placeOrder
    }
}
```

---

### REQUIRES_NEW Example

```java
@Service
public class OrderService {

    @Autowired
    private AuditService auditService;

    @Transactional
    public void placeOrder() {
        saveOrder();

        try {
            auditService.logOrder(); // new transaction
        } catch (Exception e) {
            // audit log is committed even if placeOrder later fails
        }

        // if this throws, saveOrder rolls back, but audit log remains
        throw new RuntimeException("Order failed");
    }
}

@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrder() {
        // commits independently
    }
}
```

---

### NESTED Example

```java
@Service
public class OrderService {

    @Autowired
    private InventoryService inventoryService;

    @Transactional
    public void placeOrder() {
        saveOrder(); // outer transaction

        try {
            inventoryService.reserveStock(); // nested transaction (savepoint)
        } catch (Exception e) {
            // only reserveStock rolls back to savepoint
            // saveOrder can still commit
        }
    }
}

@Service
public class InventoryService {

    @Transactional(propagation = Propagation.NESTED)
    public void reserveStock() {
        // uses savepoint
    }
}
```

---

### MANDATORY Example

```java
@Service
public class ReportService {

    @Transactional(propagation = Propagation.MANDATORY)
    public void generateReport() {
        // must be called from within a transaction
    }
}

// Calling generateReport() without a transaction throws:
// org.springframework.transaction.IllegalTransactionStateException
```

---

### NEVER Example

```java
@Service
public class NotificationService {

    @Transactional(propagation = Propagation.NEVER)
    public void sendEmail() {
        // must NOT be called inside a transaction
    }
}

// Calling sendEmail() inside a transaction throws:
// org.springframework.transaction.IllegalTransactionStateException
```

---

### SUPPORTS Example

```java
@Service
public class QueryService {

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Order> findOrders() {
        // if caller has a transaction, joins it
        // otherwise runs without a transaction
        return orderRepository.findAll();
    }
}
```

---

### NOT_SUPPORTED Example

```java
@Service
public class BatchService {

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void runBatchJob() {
        // suspends any existing transaction
        // runs without transaction
    }
}
```

---

## NESTED vs REQUIRES_NEW

| Feature | NESTED | REQUIRES_NEW |
|---|---|---|
| Uses savepoints | ✅ Yes | ❌ No |
| Inner rollback affects outer? | ❌ No (only to savepoint) | ❌ No |
| Outer rollback affects inner? | ✅ Yes | ❌ No |
| Inner commit independent? | ❌ No (part of outer) | ✅ Yes |
| Database support required | Savepoints | Any transactional DB |
| JPA / Hibernate support | Yes (if savepoints supported) | Yes |

**When to use NESTED:** You want partial rollback within the same transaction.
**When to use REQUIRES_NEW:** You want a completely independent transaction that commits even if the outer fails.

---

## Common Pitfalls

### 1. Self-invocation bypasses propagation

Calling a `@Transactional` method from within the same class bypasses the proxy, so propagation settings are ignored.

```java
@Service
public class MyService {

    public void outer() {
        inner(); // @Transactional(propagation = REQUIRES_NEW) is ignored
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inner() { ... }
}
```

**Fix:** Call from another bean, use self-injection, or use `TransactionTemplate`.

---

### 2. NESTED requires savepoint support

Not all databases support savepoints. For example, some older versions of MySQL with MyISAM do not. Use InnoDB.

---

### 3. REQUIRES_NEW can cause deadlocks

Because `REQUIRES_NEW` suspends the outer transaction and opens a new one, it can hold locks from the outer transaction while waiting for new locks, leading to deadlocks.

---

### 4. Propagation only works on public methods

With proxy-based AOP, `@Transactional` on non-public methods is ignored.

---

### 5. Catching exceptions inside the method

If you catch an exception and don’t rethrow it, the transaction may still commit even if you expected a rollback.

---

## How to Configure

### Annotation-based (recommended)

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void doSomething() { ... }
```

### XML-based (legacy)

```xml
<tx:advice id="txAdvice" transaction-manager="txManager">
    <tx:attributes>
        <tx:method name="doSomething" propagation="REQUIRES_NEW"/>
    </tx:attributes>
</tx:advice>
```

### Programmatic

```java
@Autowired
private TransactionTemplate transactionTemplate;

public void doSomething() {
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.execute(status -> {
        // logic
        return null;
    });
}
```

---

## Summary

| Propagation | Quick Description |
|---|---|
| **REQUIRED** | Join existing or create new (default) |
| **REQUIRES_NEW** | Always create new, suspend existing |
| **SUPPORTS** | Join if exists, else run without |
| **NOT_SUPPORTED** | Suspend existing, run without |
| **MANDATORY** | Must have existing, else throw |
| **NEVER** | Must not have existing, else throw |
| **NESTED** | Create savepoint within existing |

Choose the right propagation based on whether you want the method to:

- Share the caller’s transaction (`REQUIRED`, `SUPPORTS`, `MANDATORY`)
- Run independently (`REQUIRES_NEW`, `NOT_SUPPORTED`)
- Enforce presence/absence of a transaction (`MANDATORY`, `NEVER`)
- Allow partial rollback (`NESTED`)

Used correctly, propagation gives you fine-grained control over transaction boundaries in complex business flows.