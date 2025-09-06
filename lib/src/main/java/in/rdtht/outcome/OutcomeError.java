package in.rdtht.outcome;

import lombok.NonNull;

@SuppressWarnings("unused")
public sealed interface OutcomeError
        permits OutcomeError.PermissionDeniedErr,
        OutcomeError.UnknownErr,
        OutcomeError.NotFoundErr,
        OutcomeError.ValidationFailedErr,
        OutcomeError.InvalidRequestErr,
        OutcomeError.DuplicateRequestErr {

    String code();

    String message();

    Throwable origin();

    OutcomeError with(Throwable origin);

    default OutcomeError with(@NonNull OutcomeError cause) {
        Throwable wrappedOrigin = new Throwable(
                "Caused by: " + cause.code() + " - " + cause.message(),
                cause.origin() // set the old error's origin as the cause
        );
        return with(wrappedOrigin);
    }

    // --- Internal variant records ---
    record NotFoundErr(String code, String message, Throwable origin) implements OutcomeError {
        public NotFoundErr(String code, String message) {
            this(code, message, null);
        }

        @Override
        public OutcomeError with(Throwable origin) {
            return new NotFoundErr(code, message, origin);
        }
    }

    record ValidationFailedErr(String code, String message, Throwable origin) implements OutcomeError {
        public ValidationFailedErr(String code, String message) {
            this(code, message, null);
        }

        @Override
        public OutcomeError with(Throwable origin) {
            return new ValidationFailedErr(code, message, origin);
        }
    }

    record PermissionDeniedErr(String code, String message, Throwable origin) implements OutcomeError {
        public PermissionDeniedErr(String code, String message) {
            this(code, message, null);
        }

        @Override
        public OutcomeError with(Throwable origin) {
            return new PermissionDeniedErr(code, message, origin);
        }
    }

    record UnknownErr(String code, String message, Throwable origin) implements OutcomeError {
        public UnknownErr(String code, String message) {
            this(code, message, null);
        }

        @Override
        public OutcomeError with(Throwable origin) {
            return new UnknownErr(code, message, origin);
        }
    }

    record InvalidRequestErr(String code, String message, Throwable origin) implements OutcomeError {
        public InvalidRequestErr(String code, String message) {
            this(code, message, null);
        }

        @Override
        public OutcomeError with(Throwable origin) {
            return new InvalidRequestErr(code, message, origin);
        }
    }

    record DuplicateRequestErr(String code, String message, Throwable origin) implements OutcomeError {
        public DuplicateRequestErr(String code, String message) {
            this(code, message, null);
        }

        @Override
        public OutcomeError with(Throwable origin) {
            return new DuplicateRequestErr(code, message, origin);
        }
    }
}
