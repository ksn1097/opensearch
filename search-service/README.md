# Search Service

Centralized OpenSearch Search Service using:

- Java 17
- Spring Boot 3
- Maven
- OpenSearch

## Start OpenSearch

docker compose up -d

## Run Spring Boot App

mvn spring-boot:run

## APIs

### Index Document

POST /api/v1/documents

### Search Documents

POST /api/v1/search

### Delete Document

DELETE /api/v1/documents/{indexName}/{documentId}

## Sample Index Request

{
  "indexName": "payments",
  "documentId": "1",
  "document": {
    "paymentId": "pay_123",
    "status": "SUCCESS",
    "amount": 1000
  }
}

## Sample Search Request

{
  "indexName": "payments",
  "field": "status",
  "value": "SUCCESS"
}