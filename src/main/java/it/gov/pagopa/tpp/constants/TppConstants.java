package it.gov.pagopa.tpp.constants;

/**
 * Constants class containing standardized identifiers and codes for TPP operations.
 */
public class TppConstants {

    /**
     * Container class for TPP exception codes used in structured error responses.
     */
    public static final class ExceptionCode {

        public static final String TPP_NOT_ONBOARDED = "TPP_NOT_ONBOARDED";

        public static final String TPP_ALREADY_ONBOARDED = "TPP_ALREADY_ONBOARDED";
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        private ExceptionCode() {}
    }

    /**
     * Container class for TPP exception messages used in error responses and logging.
     */
    public static final class ExceptionMessage {

        public static final String TPP_NOT_ONBOARDED = "TPP_NOT_ONBOARDED";

        public static final String TPP_ALREADY_ONBOARDED = "TPP_ALREADY_ONBOARDED";
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        private ExceptionMessage() {}
    }

    /**
     * Container class for TPP exception names used in exception mapping and factory patterns.
     */
    public static final class ExceptionName {

        public static final String TPP_NOT_ONBOARDED = "TPP_NOT_ONBOARDED";

        public static final String TPP_ALREADY_ONBOARDED = "TPP_ALREADY_ONBOARDED";
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        private ExceptionName() {}
    }

    private TppConstants() {}
}
