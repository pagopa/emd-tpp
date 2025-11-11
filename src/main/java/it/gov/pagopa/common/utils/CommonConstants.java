package it.gov.pagopa.common.utils;

import java.time.ZoneId;

/**
 * Common constants class containing application-wide constant values, 
 * including exception codes and timezone configurations.
 */
public class CommonConstants {


    public static final class ExceptionCode {
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        private ExceptionCode() {}
    }

    public static final ZoneId ZONEID = ZoneId.of("Europe/Rome");

    private CommonConstants(){}
}
