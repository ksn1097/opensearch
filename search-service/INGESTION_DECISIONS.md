# Event Ingestion Decisions and Follow-ups

This file captures the technical decisions taken for the current SQS-to-OpenSearch ingestion implementation. It is intended to keep track of items that are acceptable for now, but may need improvement or architecture confirmation later.

## Decision: No Application-Level Retry for OpenSearch Bulk Indexing

### Current Decision

The application does not add a separate custom retry mechanism around OpenSearch bulk indexing.

The current flow is:

```text
SQS receives up to 50 messages
-> application parses the Debezium queue event
-> application routes by dataRecordType
-> application transforms payload to the current identity DTO shape
-> application sends one bulk indexing request to OpenSearch
-> if indexing succeeds, listener returns successfully
-> Spring Cloud AWS acknowledges/deletes the SQS messages
```

If OpenSearch indexing fails:

```text
OpenSearch bulk indexing throws an exception
-> SQS listener does not complete successfully
-> messages are not acknowledged
-> SQS visibility timeout expires
-> same messages become available for redelivery
```

The retry behavior is therefore delegated to SQS redelivery instead of being implemented manually in application code.

### Why This Is Acceptable for Now

OpenSearch indexing is currently designed to be idempotent because every document is indexed using a deterministic document ID from the message payload. For `IDENTITY`, the configured document ID field is `iiqId`.

If the same message is processed more than once, the service writes to the same OpenSearch index and the same OpenSearch document ID. This means the document is replaced or updated in place instead of creating duplicate documents.

Example:

```text
message with dataRecordType = IDENTITY and iiqId = 123
-> routed to identity_index
-> indexed with OpenSearch document ID 123

same message retried
-> routed to identity_index
-> indexed again with OpenSearch document ID 123
-> existing document is overwritten, not duplicated
```

This keeps the implementation simple while the remaining retry and DLQ architecture decisions are still being confirmed.

### Known Trade-off

The main trade-off is partial batch retry.

Example:

```text
50 messages consumed from SQS
30 documents indexed successfully in OpenSearch
20 documents fail
application treats the bulk request as failed
SQS does not acknowledge the batch
all 50 messages may be redelivered
```

In that case, the 30 successful documents may be indexed again. This should not create duplicates as long as document IDs are stable, but it can create extra OpenSearch load.

### What Needs Confirmation

Confirm with the architect whether the current SQS redelivery-based retry model is sufficient, or whether the application should implement more granular retry behavior.

Questions to confirm:

- Should OpenSearch indexing rely only on SQS redelivery and DLQ handling?
- Should the application retry failed OpenSearch calls inside the same SQS receive attempt?
- Should partial OpenSearch bulk failures be handled per document?
- Should successfully indexed messages be acknowledged even if other messages in the same batch fail?
- What should the SQS `maxReceiveCount` be before messages move to DLQ?

### Possible Future Improvements

These can be considered after the payload contract and failure-handling requirements are finalized:

- Add per-document handling for OpenSearch bulk response failures.
- Add per-message acknowledgement if we want successful messages in a batch to be deleted while failed messages are retried.
- Add an application-level retry with backoff for transient OpenSearch failures.
- Add external versioning if Debezium/event metadata provides a reliable ordering field, so older events cannot overwrite newer documents.
- Define dataRecordType-specific document ID rules for future event types.
- Add metrics for indexed, skipped, failed, and retried messages.
- Add structured logs with table name, index name, document ID, and failure reason.

## Open Question: SQS Poll and Batch Size

The current implementation is configured to process up to 50 messages in-flight per queue and up to 50 messages per listener batch.

This number needs architecture and performance confirmation.

Important SQS behavior:

- SQS returns a maximum of 10 messages per receive call.
- A framework-level batch size greater than 10 may require multiple SQS receive calls internally.
- If fewer messages are available than the configured batch size, the listener processes the available messages.
- The listener does not wait indefinitely for a full batch.
- The configured poll timeout only controls how long long-polling can wait for messages to arrive.

Example:

```text
configured batch size = 50
queue currently has 10 messages
-> application receives and processes 10 messages
-> application does not wait until 50 messages are available
```

Questions to confirm:

- What is the expected message volume per table?
- What is the expected average and maximum message size?
- What OpenSearch bulk size is recommended for this workload?
- Should the listener batch size stay at 50, or should it be increased to 100, 200, or 500?
- If batch size is increased, should the SQS visibility timeout also be increased?
- What is the acceptable retry blast radius if a large batch fails?
- Should batch size be different for migration/snapshot traffic and realtime traffic?
- Should load testing be done before finalizing the production batch size?

## Decision: Delete Handling Is Out of Scope for Current Event Contract

The current event contract does not provide an operation field, so the application does not attempt to detect insert, update, snapshot, or delete operations.

Current behavior:

```text
valid queue event
-> route by dataRecordType
-> convert payload to DTO
-> index document into OpenSearch
```

Delete handling should be revisited only if a future event contract includes delete-specific information.

### Delete Operation Follow-ups

- How will delete records be represented in the queue event?
- Will the event contract include an operation field?
- Will delete events include the same `payload` fields as normal indexing events?
- Which field should be used as the OpenSearch document ID for delete events?
- Should delete events physically remove documents from OpenSearch?
- Should delete events update the document with a soft-delete flag instead, such as `active=false` or `deleted=true`?
- If soft delete is required, what field name and value should be used?
- Should delete behavior be the same for all future table queues, or can it differ by table/index?
- If the document does not exist in OpenSearch during delete processing, should that be treated as success or failure?
- Should delete events participate in the same SQS retry/DLQ flow as insert/update events?
- Do delete events require audit logging before they are skipped, deleted, or soft-deleted?

## Decision: Route by dataRecordType

The clarified event contract includes `dataRecordType`, which represents the table/data type. The application routes to the OpenSearch index using `dataRecordType`.

Current implementation:

```text
queue event
-> dataRecordType = IDENTITY
-> target index = identity_index
```

Current configurable mapping:

```text
IDENTITY -> identity_index
```

### Routing Follow-ups

- Confirm whether DevOps will provide a separate queue for each table/dataRecordType or one queue with multiple dataRecordType values.
- If each table has a separate queue, should the application use one listener per queue?
- If all table events arrive in one queue, should every dataRecordType be mapped in one config block?
- Will the payload include the target OpenSearch index name directly?
- If the payload includes the target index name, should the application trust it or still validate it against a configured allow-list?
- Will the payload include an application/domain name in addition to table name?
- If application name is included, does routing use application name, dataRecordType, or both?
- Should routing be fully configuration-driven, or should it be hardcoded for confirmed dataRecordType values?
- What should happen when the application receives an event for an unknown dataRecordType?
- Should unknown dataRecordType messages be skipped, sent to DLQ, or stored for investigation?
- Should there be a separate queue for migration/snapshot events and realtime events, or will both arrive in the same queue?
- If migration and realtime events use the same queue, do they need different OpenSearch handling?

## DevOps Queue Details

Current DevOps-provided details:

```text
Topic ARN: arn:aws:sns:us-east-2:334672676108:ELV-IAM-OpenSearch-Events
Producer role: arn:aws:iam::334672676108:role/oneiam-ims-eks-service-account-role

Consumer queue: ONEIAM-OpenSearch-Events-To-SharedService-Queue
Consumer queue URL: https://sqs.us-east-2.amazonaws.com/334672676108/ONEIAM-OpenSearch-Events-To-SharedService-Queue

DLQ: ONEIAM-OpenSearch-Events-To-SharedService-Queue-DLQ
DLQ URL: https://sqs.us-east-2.amazonaws.com/334672676108/ONEIAM-OpenSearch-Events-To-SharedService-Queue-DLQ

Consumer role: arn:aws:iam::334672676108:role/oneiam-ssvc-eks-service-account-role
```

## Decision: Identity Document Mapping

The identity fields captured from the shared screenshots are finalized.

The current implementation converts the incoming identity payload map into `IdentityPayloadDto`, then converts that DTO back to a map for OpenSearch indexing. The OpenSearch Java client serializes that map as JSON when indexing the document.

Only fields defined in `IdentityPayloadDto` are indexed. Unknown fields from the incoming payload are ignored.

Current stable OpenSearch document ID field:

```text
iiqId
```

### Current Parser Contract

The parser expects the outer event shape:

```text
correlationId
dataRecordType
payload
source
```

Routing is currently based on:

```text
dataRecordType
```

The `payload` object is converted to the identity DTO and then indexed into OpenSearch.
