# Event Ingestion Decisions and Follow-ups

This file captures the technical decisions taken for the current SQS-to-OpenSearch ingestion implementation. It is intended to keep track of items that are acceptable for now, but may need improvement or architecture confirmation later.

## Decision: No Application-Level Retry for OpenSearch Bulk Indexing

### Current Decision

The application does not add a separate custom retry mechanism around OpenSearch bulk indexing.

The current flow is:

```text
SQS receives up to 50 messages
-> application parses and routes messages by table name
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

OpenSearch indexing is currently designed to be idempotent because every document is indexed using a deterministic document ID from the message payload, such as `documentId`, `document_id`, `id`, or a table-specific ID field.

If the same message is processed more than once, the service writes to the same OpenSearch index and the same OpenSearch document ID. This means the document is replaced or updated in place instead of creating duplicate documents.

Example:

```text
message for spt_identity with id = 123
-> routed to identity_index
-> indexed with OpenSearch document ID 123

same message retried
-> routed to identity_index
-> indexed again with OpenSearch document ID 123
-> existing document is overwritten, not duplicated
```

This keeps the implementation simple while the payload contract and final architecture decisions are still being confirmed.

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
- Define table-specific document ID rules once the final payload shape is confirmed.
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

## Decision: Delete Events Are Ignored for Now

Delete handling is not implemented yet because the architecture still needs confirmation on whether delete events should remove documents from OpenSearch or mark them inactive.

Current behavior:

```text
payload.op = d
-> message is skipped
-> no OpenSearch delete is performed
```

This should be revisited once the delete strategy is finalized.

### Delete Operation Questions to Confirm

- Should delete events physically remove documents from OpenSearch?
- Should delete events update the document with a soft-delete flag instead, such as `active=false` or `deleted=true`?
- If soft delete is required, what field name and value should be used?
- Should delete behavior be the same for all tables, or can it differ by table/index?
- If the document does not exist in OpenSearch during delete processing, should that be treated as success or failure?
- Should delete events participate in the same SQS retry/DLQ flow as insert/update events?
- Do delete events require audit logging before they are skipped, deleted, or soft-deleted?
- Should delete events be ignored only temporarily, or should the application store them somewhere for replay once delete handling is finalized?

## Open Question: Queue and Routing Contract

The current implementation assumes a single SQS queue can contain messages for multiple tables. The application reads the table name from the message payload and maps it to the target OpenSearch index using configuration.

Current assumption:

```text
one shared SQS queue
-> message contains table/application/routing information
-> application maps table name to OpenSearch index
```

Current configurable mapping:

```text
spt_identity -> identity_index
spt_identity_entitlement -> identity_entitlement_index
spt_application -> application_index
```

This needs confirmation because the upstream Debezium parsing service may choose a different contract.

### Routing Questions to Confirm

- Will all table events arrive in one shared queue, or will each table have a separate queue?
- If each table has a separate queue, should the application use one listener per queue?
- If each table has a separate queue, do we still need table name in the payload?
- Will the message payload include the source table name?
- What will the exact table field be called: `table`, `tableName`, `source.table`, or something else?
- Will the payload include the target OpenSearch index name directly?
- If the payload includes the target index name, should the application trust it or still validate it against a configured allow-list?
- Will the payload include an application/domain name in addition to table name?
- If application name is included, does routing use application name, table name, or both?
- Should routing be fully configuration-driven, or should it be hardcoded for the confirmed IIQ tables?
- What should happen when the application receives an event for an unknown table?
- Should unknown-table messages be skipped, sent to DLQ, or stored for investigation?
- Should there be a separate queue for migration/snapshot events and realtime events, or will both arrive in the same queue?
- If migration and realtime events use the same queue, do they need different OpenSearch handling?

### Current Implementation Until Confirmed

Until the final contract is confirmed, the parser checks common table-name locations:

```text
payload.tableName
payload.table
payload.source.table
root.tableName
root.table
```

The record body is read from the first available field:

```text
payload.record
payload.data
payload.document
payload.after
```

If none of those fields exist, the full `payload` object is indexed.
