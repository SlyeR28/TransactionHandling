# `@Transactional` in Spring Boot

## What it does

`@Transactional` is used for **declarative transaction management** in Spring Boot.

It ensures that multiple database operations inside a method are executed as **one single unit of work**.

- If everything succeeds → transaction is **committed**
- If a rollback-triggering exception occurs → transaction is **rolled back**
- Rollback means all database changes made inside that transaction are undone

In simple terms:

> If multiple DB operations are performed in one method annotated with `@Transactional`, they are treated as one transaction. If any operation fails and the exception is allowed to propagate, the whole transaction can be rolled back, so nothing is saved in the database.

But there is an important caveat: **Spring does not roll back for every exception by default.**

---

## ACID Properties

`@Transactional` helps manage database transactions, and transactions generally follow ACID properties.

| Property | Meaning |
|---|---|
| **A – Atomicity** | All operations succeed, or none succeed. It is all-or-nothing. |
| **C – Consistency** | The database remains in a valid/consistent state before and after the transaction. |
| **I – Isolation** | Multiple transactions running in parallel should not interfere with each other. |
| **D – Durability** | Once a transaction is committed, the data should not be lost even if the system fails. |

---

## Default Rollback Behavior in Spring

Spring’s default rollback rule is important:

| Exception Type | Default Rollback? |
|---|---|
| `RuntimeException` and its subclasses | ✅ Yes |
| `Error` and its subclasses | ✅ Yes |
| Checked exceptions | ❌ No |

So:

- If a **runtime exception** occurs → transaction rolls back by default.
- If a **checked exception** occurs → transaction does **not** roll back by default. It may commit.
- If a **network issue** throws a checked exception like `IOException` → it will **not** roll back by default unless configured.
- If the exception is caught inside the method and not rethrown → Spring never sees it, so the transaction commits.

---

## Runtime Exception Example

```java
@Service
public class OrderService {

    @Transactional
    public void placeOrder() {
        saveOrder();
        savePayment();

        // RuntimeException is unchecked, so Spring rolls back by default
        throw new RuntimeException("Something failed");
    }
}
```

Result: both `saveOrder()` and `savePayment()` are rolled back.

---

## Checked Exception Problem

```java
@Service
public class OrderService {

    @Transactional
    public void placeOrder() throws IOException {
        saveOrder();
        savePayment();

        // IOException is checked.
        // By default, Spring will NOT roll back for this.
        throw new IOException("Network failure");
    }
}
```

Result by default: the transaction may still be **committed**, even though an exception was thrown.

To fix this, use `rollbackFor`:

```java
@Transactional(rollbackFor = Exception.class)
public void placeOrder() throws IOException {
    saveOrder();
    savePayment();

    throw new IOException("Network failure");
}
```

Now checked exceptions also trigger rollback.

You can also be more specific:

```java
@Transactional(rollbackFor = IOException.class)
public void placeOrder() throws IOException {
    // ...
}
```

---

## Complete Example

```java
@Service
public class BankService {

    private final AccountRepository accountRepository;

    public BankService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public void transferMoney(Long fromAccountId, Long toAccountId, BigDecimal amount)
            throws IOException {

        accountRepository.debit(fromAccountId, amount);
        accountRepository.credit(toAccountId, amount);

        // If any exception occurs, including checked exceptions,
        // the whole transaction will roll back.
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IOException("Invalid amount");
        }
    }
}
```

---

## Important Pitfalls

### Self-invocation does not work

If a method inside the same class calls another `@Transactional` method, the annotation may be bypassed because Spring uses proxies.

```java
@Service
public class MyService {

    public void outer() {
        inner(); // @Transactional on inner() is bypassed
    }

    @Transactional
    public void inner() {
        // transaction may not start
    }
}
```

Fix: call the transactional method from another Spring bean, use self-injection, or use `TransactionTemplate`.

---

### Method visibility

With default proxy-based Spring AOP, `@Transactional` works on **public methods**.

- `public` → works
- `private` / `protected` / package-private → usually ignored by proxy-based transaction management

Make the method `public`.

---

### Catching the exception inside the method

```java
@Transactional
public void process() {
    try {
        saveData();
        throw new RuntimeException("Failed");
    } catch (Exception e) {
        // Exception is swallowed
        // Spring sees normal return, so transaction commits
    }
}
```

If you catch the exception and do not rethrow it, Spring will not roll back.

Fix: rethrow the exception or mark the transaction as rollback-only.

---

### Checked exceptions

As explained above, checked exceptions do not roll back by default.

Use:

```java
@Transactional(rollbackFor = Exception.class)
```

or a specific checked exception class.

---

### Multiple databases / transaction managers

If your application uses multiple databases or multiple `PlatformTransactionManager` beans, you may need to specify which transaction manager to use:

```java
@Transactional(transactionManager = "orderTransactionManager")
```

---

### Propagation behavior

Default propagation is `REQUIRED`.

This means:

- If a transaction already exists, the method joins it.
- If no transaction exists, a new one is created.

Other propagation types like `REQUIRES_NEW`, `NESTED`, `SUPPORTS`, etc., change how transactions behave.

Example:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

---

### Database support

Transactions only work if the underlying database supports them.

For example, MySQL’s `MyISAM` engine does not support transactions. Use `InnoDB`.

---

## Summary Table

| Scenario | Default Behavior | Solution |
|---|---|---|
| `RuntimeException` | Rolls back | No change needed |
| `Error` | Rolls back | No change needed |
| Checked exception | Does not roll back | `@Transactional(rollbackFor = Exception.class)` |
| Exception caught inside method | Commits | Rethrow or mark rollback-only |
| Self-invocation | Transaction bypassed | Call through another bean or use `TransactionTemplate` |
| Non-public method | Usually ignored | Make method `public` |
| Multiple transaction managers | May fail or use wrong one | Specify `transactionManager` |
| Database without transaction support | No rollback | Use a transactional database engine |

---

## Final Note

`@Transactional` is not magic. It relies on:

- Spring AOP proxies
- Correct method visibility
- Correct exception propagation
- Proper rollback rules
- Underlying database transaction support

Used correctly, it gives you atomic database operations with commit on success and rollback on failure.