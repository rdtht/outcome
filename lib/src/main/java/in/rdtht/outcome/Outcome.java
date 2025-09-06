package in.rdtht.outcome;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public sealed interface Outcome<T> permits Outcome.Ok, Outcome.Err, Outcome.None {
    String UNEXPECTED_ERROR = "UNEXPECTED_ERROR";
    String UNEXPECTED_ERROR_MESSAGE = "Failed to finish the operation";

    // --- utility record for pairing when zipped ---
    record Pair<F, S>(F first, S second) {
    }

    // ----------------
    // --- Variants ---
    // ----------------

    record Ok<T>(T value) implements Outcome<T> {
        public Ok {
            Objects.requireNonNull(value, "Ok value cannot be null");
        }
    }

    record Err<T>(OutcomeError error) implements Outcome<T> {
        public Err {
            Objects.requireNonNull(error, "Error cannot be null");
        }
    }

    record None<T>() implements Outcome<T> {
    }

    // ---------------------------
    // --- Static constructors ---
    // ---------------------------

    static <T> Outcome<T> ok(@NonNull T value) {
        return new Ok<>(value);
    }

    static <T> Outcome<T> err(@NonNull OutcomeError error) {
        return new Err<>(error);
    }

    static <T> Outcome<T> none() {
        return new None<>();
    }

    static <T> Outcome<T> from(@NonNull Supplier<T> supplier) {
        try {
            T val = supplier.get();
            if (val == null) return new None<>();
            return new Ok<>(val);
        } catch (RuntimeException ex) {
            Throwable e = new RuntimeException("Outcome suppler call failed", ex);
            return new Err<>(new OutcomeError.UnknownErr(UNEXPECTED_ERROR, UNEXPECTED_ERROR_MESSAGE, e));
        }
    }

    // ---------------------------
    // -------- Utility ----------
    // ---------------------------

    static <T> Outcome<T> flatten(@NonNull Outcome<Outcome<T>> nested) {
        if (nested instanceof Ok<Outcome<T>>(Outcome<T> inner)) {
            return inner;
        }
        if (nested instanceof Err<Outcome<T>>(OutcomeError error)) {
            return new Err<>(error);
        }
        return new None<>();
    }

    static <T, U> Outcome<List<U>> traverse(
            @NonNull List<T> list,
            @NonNull Function<T, Outcome<U>> mapper
    ) {
        return sequence(list.stream().map(mapper).collect(Collectors.toList()));
    }

    static <T> Outcome<List<T>> sequence(@NonNull List<Outcome<T>> outcomes) {
        List<T> results = new ArrayList<>();
        for (Outcome<T> outcome : outcomes) {
            if (outcome instanceof Ok<T>(T value)) {
                results.add(value);
            } else if (outcome instanceof Err<T>(OutcomeError error)) {
                return new Err<>(error);
            } else {
                return new None<>();
            }
        }
        return Outcome.ok(results);
    }

    // ----------------------------
    // -------- Predicates --------
    // ----------------------------

    default boolean isOk() {
        return this instanceof Ok<T>;
    }

    default boolean isErr() {
        return this instanceof Err<T>;
    }

    default boolean isNone() {
        return this instanceof None<T>;
    }

    // ----------------------------
    // -------- Extractors --------
    // ----------------------------

    default T getOrElse(@NonNull T other) {
        return this instanceof Ok<T>(T value) ? value : other;
    }

    default T getOrElseGet(@NonNull Supplier<T> supplier) {
        return this instanceof Ok<T>(T value) ? value : supplier.get();
    }

    /// #### Unwraps the value of the outcome only if Ok.
    /// @throws RuntimeException if not Ok
    default T unwrap() {
        if (this instanceof Ok<T>(T value)) return value;
        if (this instanceof Err<T>(OutcomeError error))
            throw new RuntimeException("Outcome unwrap failed", error.origin());
        throw new NullPointerException("Outcome unwrap failed with null value");
    }

    // --- Fold: handle all 3 states in one go ---
    default <U> U fold(
            @NonNull Function<T, U> onOk,
            @NonNull Function<OutcomeError, U> onErr,
            @NonNull Supplier<U> onNone
    ) {
        if (this instanceof Ok<T>(T value)) return onOk.apply(value);
        if (this instanceof Err<T>(OutcomeError error)) return onErr.apply(error);
        return onNone.get();
    }

    // ----------------------------
    // ----- Transformations ------
    // ----------------------------

    default <U> Outcome<U> replace(U obj) {
        if (this instanceof Err<T>(OutcomeError error)) return new Err<>(error);
        return obj != null ? new Ok<>(obj) : Outcome.none();
    }

    default <U> Outcome<U> map(@NonNull Function<T, U> mapper) {
        return flatMap(value -> new Ok<>(mapper.apply(value)));
    }

    default <U> Outcome<U> flatMap(@NonNull Function<T, Outcome<U>> mapper) {
        try {
            if (this instanceof Ok<T>(T value)) {
                var newOutcome = mapper.apply(value);
                if (newOutcome == null) return Outcome.none();
                return newOutcome;
            }
            if (this instanceof Err<T>(OutcomeError error)) return new Err<>(error);
            return new None<>();
        } catch (RuntimeException e) {
            RuntimeException ex = new RuntimeException("Outcome flatMap mapper call failed", e);
            return new Err<>(new OutcomeError.UnknownErr(UNEXPECTED_ERROR, UNEXPECTED_ERROR_MESSAGE, ex));
        }
    }

    default Outcome<T> mapErr(@NonNull Function<OutcomeError, OutcomeError> mapper) {
        if (this instanceof Err<T>(OutcomeError error)) {
            OutcomeError mapped = mapper.apply(error);
            Objects.requireNonNull(mapped, "Error mapper cannot return null");
            // Wrap old error as cause if they are not the same object
            if (mapped != error) mapped = mapped.with(error);
            return new Err<>(mapped);
        }
        return this;
    }

    // --- Maps none outcome to an error ---
    default Outcome<T> orElseErr(@NonNull OutcomeError newError) {
        if (this instanceof None<T>) return new Err<>(newError);
        return this;
    }

    default <U> Outcome<Pair<T, U>> zip(@NonNull Outcome<U> other) {
        if (this instanceof Ok<T>(T value1) && other instanceof Ok<U>(U value2)) {
            return Outcome.ok(new Pair<>(value1, value2));
        }
        if (this instanceof Err<T>(OutcomeError error)) return new Err<>(error);
        if (other instanceof Err<U>(OutcomeError error)) return new Err<>(error);
        return new None<>();
    }

    default Outcome<T> filter(@NonNull Predicate<T> predicate, @NonNull OutcomeError errorIfFalse) {
        try {
            if (this instanceof Ok<T>(T value))
                return predicate.test(value) ? this : new Err<>(errorIfFalse);

            return this;
        } catch (RuntimeException e) {
            RuntimeException ex = new RuntimeException("Outcome filter predicate call failed", e);
            return new Err<>(new OutcomeError.UnknownErr(UNEXPECTED_ERROR, UNEXPECTED_ERROR_MESSAGE, ex));
        }
    }

    // ----------------------------
    // ------- Side effects -------
    // ----------------------------

    default void ifOk(@NonNull Consumer<T> action) {
        if (this instanceof Ok<T>(T value)) action.accept(value);
    }

    default void ifErr(@NonNull Consumer<OutcomeError> action) {
        if (this instanceof Err<T>(OutcomeError error)) action.accept(error);
    }

    default void ifNone(@NonNull Runnable action) {
        if (this instanceof None<T>) action.run();
    }

    default Outcome<T> tap(@NonNull Consumer<T> action) {
        if (this instanceof Ok<T>(T value)) {
            try {
                action.accept(value);
            } catch (RuntimeException e) {
                RuntimeException ex = new RuntimeException("Outcome peek failed", e);
                return new Err<>(new OutcomeError.UnknownErr(UNEXPECTED_ERROR, UNEXPECTED_ERROR_MESSAGE, ex));
            }
        }
        return this;
    }

    // --- Chain an operation that returns Outcome<U> but preserve the original value T ---
    // --- Returns Outcome<T> if both operations succeed, or the first error encountered ---
    default <U> Outcome<T> tapWith(@NonNull Function<T, Outcome<U>> mapper) {
        if (this instanceof Ok<T>(T value)) {
            try {
                var result = mapper.apply(value);

                if (result instanceof Err<U>(OutcomeError error)) {
                    return new Err<>(error);
                }

                if (result == null) return new Err<>(new OutcomeError.UnknownErr(
                            UNEXPECTED_ERROR, "Outcome mapper returned null", null));

                return this;
            } catch (RuntimeException e) {
                RuntimeException ex = new RuntimeException("Outcome peekAndThen call failed", e);
                return new Err<>(new OutcomeError.UnknownErr(UNEXPECTED_ERROR, UNEXPECTED_ERROR_MESSAGE, ex));
            }
        }
        if (this instanceof Err<T>(OutcomeError error)) return new Err<>(error);
        return new None<>();
    }

    // ----------------------------
    // ------- Err Recovery -------
    // ----------------------------

    default Outcome<T> recover(@NonNull Function<OutcomeError, T> recovery) {
        if (this instanceof Err<T>(OutcomeError error)) {
            try {
                T recovered = recovery.apply(error);
                return recovered != null ? Outcome.ok(recovered) : Outcome.none();
            } catch (RuntimeException e) {
                Throwable combined = new RuntimeException("Outcome recovery failed", error.origin());
                combined.addSuppressed(e);
                return new Err<>(error.with(combined));
            }
        }
        return this;
    }

    default Outcome<T> recoverWith(@NonNull Function<OutcomeError, Outcome<T>> mapper) {
        if (this instanceof Err<T>(OutcomeError error)) {
            try {
                var result = mapper.apply(error);
                return result != null ? result : Outcome.none();
            } catch (RuntimeException e) {
                Throwable combined = new RuntimeException("Outcome recovery mapper failed", error.origin());
                combined.addSuppressed(e);
                return new Err<>(error.with(combined));
            }
        }
        return this;
    }
}