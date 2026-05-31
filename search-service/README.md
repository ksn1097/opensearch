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

The service also listens to the SQS queue configured by `app.sqs.debezium-queue-name`.
This queue is expected to receive already-parsed Debezium routed payloads directly or through an SNS subscription envelope.

Configured table routing:

- `spt_identity` -> `identity_index`
- `spt_identity_entitlement` -> `identity_entitlement_index`
- `spt_application` -> `application_index`

Insert, update, and snapshot-style payloads are indexed into the configured table index.
Delete events are ignored for now.

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
