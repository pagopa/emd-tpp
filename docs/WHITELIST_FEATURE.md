# Whitelist Management for TPPs (MMC-339)

This feature allows for managing a "whitelist" of recipient IDs for each TPP. 
Its primary purpose is to allow specific users (recipients) to test TPP integrations even when the TPP is not yet globally enabled (`state: false`).

## Business Logic

A TPP is considered "active" for a specific request if:
1.  The TPP's global state is active (`state: true`).
2.  **OR** the TPP is disabled but the requesting `recipientId` is present in the TPP's `whitelistRecipient` list.

## Technical Implementation

### Model Changes
The `Tpp` document in MongoDB now includes:
- `whitelistRecipient`: A list of strings containing recipient identifiers.

### API Endpoints

#### Listing TPPs
- **Endpoint**: `POST /emd/tpp/list`
- **Request Body**: 
  ```json
  {
    "ids": ["tppId1", "tppId2"],
    "recipientId": "user_id_here"
  }
  ```
- **Behavior**: Returns TPPs that are either active OR have the specified `recipientId` in their whitelist.

#### Whitelist Administration
- `GET /emd/tpp/whitelist`: Returns all whitelists across all TPPs.
- `GET /emd/tpp/{tppId}/whitelist`: Returns the whitelist for a specific TPP.
- `POST /emd/tpp/{tppId}/whitelist`: Adds a recipient to the whitelist.
  - Body: `{"recipientId": "string"}`
- `DELETE /emd/tpp/{tppId}/whitelist/{recipientId}`: Removes a recipient from the whitelist.
- `PUT /emd/tpp/{tppId}/whitelist`: Overwrites the entire whitelist for a TPP.
  - Body: `["id1", "id2", ...]`

### Database Optimization
A custom query has been implemented in `TppRepository` to efficiently fetch TPPs based on the OR condition between state and whitelist membership using `@Query`.

## Tests
Unit tests have been updated in:
- `TppServiceTest`: Verification of filtering logic with whitelisted recipients.
- `TppControllerTest`: Verification of new REST endpoints.