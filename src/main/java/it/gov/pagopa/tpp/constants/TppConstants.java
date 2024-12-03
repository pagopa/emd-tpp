package it.gov.pagopa.tpp.constants;

public class TppConstants {
    public static final class ExceptionCode {

        public static final String TPP_NOT_ONBOARDED = "TPP_NOT_ONBOARDED";

        public static final String TPP_ALREADY_ONBOARDED = "TPP_ALREADY_ONBOARDED";
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        public static final String TPP_NOT_READY = "TPP_NOT_READY";

        private ExceptionCode() {}
    }

    public static final class ExceptionMessage {

        public static final String TPP_NOT_ONBOARDED = "TPP_NOT_ONBOARDED";

        public static final String TPP_ALREADY_ONBOARDED = "TPP_ALREADY_ONBOARDED";
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        public static final String TPP_NOT_READY = "TPP_NOT_READY";
        private ExceptionMessage() {}
    }

    public static final class ExceptionName {

        public static final String TPP_NOT_ONBOARDED = "TPP_NOT_ONBOARDED";

        public static final String TPP_ALREADY_ONBOARDED = "TPP_ALREADY_ONBOARDED";
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        public static final String TPP_NOT_READY = "TPP_NOT_READY";

        private ExceptionName() {}
    }


    private TppConstants() {}
}
