# Outcome API

A lightweight, type-safe alternative to exceptions in Java.  
`Outcome<T>` represents the result of an operation that can succeed (`Ok`), fail (`Err`), or yield no result (`None`).  
It provides functional-style utilities for mapping, chaining, recovering, and handling errors in a declarative way.

---

## ✨ Features

- **Three-state result handling**: `Ok<T>`, `Err<T>`, `None<T>`
- **Typed errors** with `OutcomeError` variants (e.g., `NotFoundErr`, `ValidationFailedErr`)
- **Functional transformations**: `map`, `flatMap`, `filter`, `zip`
- **Error recovery**: `recover`, `recoverWith`, `mapErr`
- **Collection utilities**: `sequence`, `traverse`, `flatten`
- **Side-effect hooks**: `ifOk`, `ifErr`, `ifNone`, `tap`, `tapWith`
- Null-safe by design (`Outcome.none()` instead of `null`)
- No external runtime dependencies (uses only JDK + Lombok for annotations)

---

## 🔑 Usage

### Basic construction

```java
Outcome<String> success = Outcome.ok("Hello World");
Outcome<String> failure = Outcome.err(
    new OutcomeError.NotFoundErr("USER_NOT_FOUND", "User not found")
);
Outcome<String> empty = Outcome.none();
```

### Unwrapping

```java
String value = success.getOrElse("default"); // "Hello World"
String fallback = empty.getOrElse("default"); // "default"

// Throws RuntimeException if not Ok
String unwrapped = success.unwrap();
```

### Chaining with `map` / `flatMap`

```java
Outcome<Integer> result = Outcome.ok("42")
    .map(Integer::parseInt)      // Ok(42)
    .flatMap(num -> num > 0
        ? Outcome.ok(num * 2)
        : Outcome.err(new OutcomeError.ValidationFailedErr("NEGATIVE", "Number must be positive"))
    );
```

### Error handling

```java
var outcome = Outcome
        .<String>err(new OutcomeError.ValidationFailedErr("INVALID", "Bad input"))
        .recover(err -> "Recovered value")
        .mapErr(err -> new OutcomeError.UnknownErr("WRAPPED", "Wrapped error"));
```

### Side effects

```java
Outcome.ok("data")
    .ifOk(val -> System.out.println("Value = " + val))
    .ifErr(err -> System.err.println("Error = " + err.message()))
    .ifNone(() -> System.out.println("No value present"));
```

### Working with collections

```java
List<Outcome<Integer>> outcomes = List.of(
    Outcome.ok(1),
    Outcome.ok(2),
    Outcome.ok(3)
);

Outcome<List<Integer>> sequenced = Outcome.sequence(outcomes);
// -> Ok([1, 2, 3])
```

---

## ⚡ OutcomeError Variants

Errors are strongly typed and immutable:

- `NotFoundErr`
- `ValidationFailedErr`
- `PermissionDeniedErr`
- `InvalidRequestErr`
- `DuplicateRequestErr`
- `UnknownErr`

Each error has:

```java
String code();     // machine-readable code
String message();  // human-readable message
Throwable origin(); // optional underlying exception
```

---

## 📚 Motivation

Exceptions in Java are often overused for control flow, leading to messy, nested try/catch blocks.  
`Outcome<T>` provides:

- **Explicit success/failure contracts** in method signatures
- **No hidden control flow** (errors are values, not thrown)
- **Functional composition** for building pipelines

This pattern is inspired by Rust’s `Result`, Kotlin’s `Result`, and Haskell’s `Either`.

---

## 🛠 Roadmap

- [ ] Add async support (`CompletableFuture<Outcome<T>>` helpers)  
- [ ] Provide Jackson / Gson modules for JSON serialization  
- [ ] Expand error taxonomy  

---

## 🤝 Contributing

Contributions are welcome!  
Feel free to open issues or submit PRs for bug fixes, enhancements, or new features.
