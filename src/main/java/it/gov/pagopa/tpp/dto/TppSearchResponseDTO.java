package it.gov.pagopa.tpp.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a paginated result of a TPP search operation.
 * <p>
 * It carries the page content together with the pagination metadata needed by the client
 * to navigate the result set (current page, page size, total elements and total pages).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TppSearchResponseDTO {

    /**
     * The list of TPPs (without token section) belonging to the current page.
     */
    private List<TppDTOWithoutTokenSection> content;

    /**
     * The zero-based index of the current page.
     */
    private int page;

    /**
     * The number of elements requested per page.
     */
    private int size;

    /**
     * The total number of elements matching the search criteria across all pages.
     */
    private long totalElements;

    /**
     * The total number of pages available for the given page size.
     */
    private int totalPages;
}

