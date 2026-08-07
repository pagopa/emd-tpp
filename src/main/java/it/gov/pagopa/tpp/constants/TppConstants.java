package it.gov.pagopa.tpp.constants;

import java.util.Set;

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

        public static final String RECIPIENT_NOT_FOUND = "RECIPIENT_NOT_FOUND";
        public static final String RECIPIENT_ALREADY_PRESENT = "RECIPIENT_ALREADY_PRESENT";

        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        public static final String INVALID_SEARCH_FIELD = "INVALID_SEARCH_FIELD";
        private ExceptionCode() {}
    }

    /**
     * Container class for TPP exception messages used in error responses and logging.
     */
    public static final class ExceptionMessage {

        public static final String TPP_NOT_ONBOARDED = "TPP_NOT_ONBOARDED";
        public static final String TPP_NOT_FOUND = "Tpp not found during get process";
        public static final String TPP_ALREADY_ONBOARDED = "TPP_ALREADY_ONBOARDED";

        public static final String RECIPIENT_NOT_FOUND = "RECIPIENT_NOT_FOUND";
        public static final String RECIPIENT_ALREADY_PRESENT = "RECIPIENT_ALREADY_PRESENT";

        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        public static final String INVALID_SEARCH_FIELD = "One or more requested 'fields' are not allowed for search projection";
        private ExceptionMessage() {}
    }

    /**
     * Container class for TPP exception names used in exception mapping and factory patterns.
     */
    public static final class ExceptionName {

        public static final String TPP_NOT_ONBOARDED = "TPP_NOT_ONBOARDED";
        public static final String TPP_ALREADY_ONBOARDED = "TPP_ALREADY_ONBOARDED";

        public static final String RECIPIENT_NOT_FOUND = "RECIPIENT_NOT_FOUND";
        public static final String RECIPIENT_ALREADY_PRESENT = "RECIPIENT_ALREADY_PRESENT";

        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        public static final String INVALID_SEARCH_FIELD = "INVALID_SEARCH_FIELD";
        private ExceptionName() {}
    }

    /**
     * Container class defining the fields available in {@code TppDTOWithoutTokenSection} that
     * can be requested for projection in the {@code searchTpps} operation, together with the
     * default set of fields returned when no override is provided (the ones shown in the grid).
     */
    public static final class SearchFields {

        public static final String TPP_ID = "tppId";
        public static final String CLIENT_ID = "clientId";
        public static final String ENTITY_ID = "entityId";
        public static final String ID_PSP = "idPsp";
        public static final String BUSINESS_NAME = "businessName";
        public static final String LEGAL_ADDRESS = "legalAddress";
        public static final String MESSAGE_URL = "messageUrl";
        public static final String AUTHENTICATION_URL = "authenticationUrl";
        public static final String AUTHENTICATION_TYPE = "authenticationType";
        public static final String CONTACT = "contact";
        public static final String STATE = "state";
        public static final String CREATION_DATE = "creationDate";
        public static final String LAST_UPDATE_DATE = "lastUpdateDate";
        public static final String PSP_DENOMINATION = "pspDenomination";
        public static final String AGENT_LINKS = "agentLinks";
        public static final String IS_PAYMENT_ENABLED = "isPaymentEnabled";
        public static final String MESSAGE_TEMPLATE = "messageTemplate";
        public static final String WHITELIST_RECIPIENT = "whitelistRecipient";

        /**
         * All the fields that can be requested through the {@code fields} search parameter.
         */
        public static final Set<String> ALLOWED = Set.of(
                TPP_ID, CLIENT_ID, ENTITY_ID, ID_PSP, BUSINESS_NAME, LEGAL_ADDRESS,
                MESSAGE_URL, AUTHENTICATION_URL, AUTHENTICATION_TYPE, CONTACT, STATE,
                CREATION_DATE, LAST_UPDATE_DATE, PSP_DENOMINATION, AGENT_LINKS,
                IS_PAYMENT_ENABLED, MESSAGE_TEMPLATE, WHITELIST_RECIPIENT
        );

        /**
         * Default fields returned by {@code searchTpps} when no {@code fields} override is
         * provided, matching the columns shown in the TPP grid.
         */
        public static final Set<String> DEFAULT_GRID_FIELDS = Set.of(
                BUSINESS_NAME, ENTITY_ID, IS_PAYMENT_ENABLED, TPP_ID, STATE, LAST_UPDATE_DATE
        );

        private SearchFields() {}
    }

    private TppConstants() {}
}
