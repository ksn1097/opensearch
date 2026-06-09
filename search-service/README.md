# Search Service

Centralized OpenSearch Search Service using:

- Java 17
- Spring Boot 3
- Maven
- OpenSearch
- SQS event ingestion for Debezium events

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

## Debezium Event Ingestion

The service also listens to the SQS queue configured by `app.sqs.identity-queue-name`.
This queue is expected to receive Debezium queue events directly or through an SNS subscription envelope.

Expected event shape:

```json
{
  "correlationId": "...",
  "dataRecordType": "IDENTITY",
  "payload": {},
  "source": "..."
}
```

Configured routing:

- `dataRecordType=IDENTITY` -> `identity_index`

Received payloads are converted to the finalized identity DTO shape and indexed.

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
