# emd-tpp
Service that manages the registration and data recovery operations of TPPs.

The microservice is responsible for:
- managing the registration or modification of third-party data
- changing the activation status of the courtesy message service by the third party
- retrieving TTP data
- updating data used for token generation

## API Documentation

API specification: [openapi.tpp.yml](https://github.com/pagopa/cstar-infrastructure/blob/main/src/domains/mil-app-poc/api/emd_tpp/openapi.tpp.yml)


## Components

---

#### [TppService](src/main/java/it/gov/pagopa/tpp/service/TppServiceImpl.java)

Main class that handles CRUD operations with TPP entities.

---

#### [TppRepository](src/main/java/it/gov/pagopa/tpp/repository/TppRepository.java)

Repository used for operations performed on the database

Collection used: 'tpp'