package it.gov.pagopa.tpp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class TppIdList {
    List<String> ids;
}
