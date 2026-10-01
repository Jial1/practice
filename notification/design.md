# Notification Preferences Design

## Scope

Implement preference retrieval and updates only. Notification delivery is out of scope. Authentication is assumed to provide the current `userId`; clients do not choose the user ID.

## API Contract

- `GET /users/me/notification-preferences` returns effective values for email, SMS, and push. Missing stored rows resolve to `true`.
- `PATCH /users/me/notification-preferences` accepts one or more channel fields, for example `{ "emailEnabled": false }`. Only supplied fields change; missing rows for other channels still resolve to `true`.
- Reject an empty PATCH. Return the updated effective representation on success.
- Updates to existing same-channel rows use last-committed-write-wins; updates to different channels must not overwrite one another. If concurrent PATCH requests both try to create the same missing row, the unique constraint makes one fail with `409 Conflict`; the client may retry the idempotent PATCH.

## Persistence Model

Use Spring Data JPA with Hibernate and one row per customized user/channel preference. `NotificationPreferenceEntity` maps the table; `NotificationPreferenceRepository` derives the user lookup from its method name, avoiding handwritten query SQL.

- A generated row ID is the JPA entity key; enforce uniqueness on `(user_id, channel)`.
- Map required entity fields with `@Column(nullable = false)` and keep matching `NOT NULL` constraints in `schema.sql`; the database constraint is the final persistence safeguard.
- Persist `channel` as its enum name with `@Enumerated(EnumType.STRING)` and constrain allowed values in the database.
- Use Lombok narrowly on the entity for field getters and JPA's protected no-argument constructor; retain an explicit constructor for valid application-created entities. Avoid `@Data` and generated setters.
- `channel` must be one of `EMAIL`, `SMS`, or `PUSH`; enforce in application validation and a database constraint.
- `enabled` is non-null.
- Absence of a row means enabled (`true`); PATCH creates or updates rows only for supplied fields.
- Apply a multi-field PATCH atomically. Repository writes must handle concurrent row creation without duplicate rows or lost updates to other channels.

H2 is the local runtime database. `schema.sql` creates the table and constraints; Hibernate validates the mapping at startup. The production database and versioned migration strategy remain deployment decisions.

## Implementation Structure

Java packages under `com.example.notification` follow the layer boundaries:

- `controller/NotificationPreferenceController`: GET/PATCH routes and HTTP mapping.
- `dto`: response and partial-update request types.
- `service`: authenticated user scoping, defaults, update orchestration, transaction boundary.
- `domain/NotificationChannel`: supported channel identifiers and preference rules.
- `repository/NotificationPreferenceRepository`: Spring Data JPA user lookup and persistence operations.
- `persistence/NotificationPreferenceEntity`: JPA entity mapping; SQL constraints are in `src/main/resources/schema.sql`.
- `NotificationApplication` remains in the root package so Spring component and entity scanning includes each layer package.

Keep delivery integrations, caching, and asynchronous messaging out of this phase.

## Code Conventions

- Prefer focused annotations when they clearly express framework behavior or replace repetitive boilerplate, such as JPA mapping constraints and Lombok getters. Avoid broad annotations that generate unnecessary entity APIs (for example, Lombok `@Data`); keep constructors explicit when they enforce valid entity creation.

## Implementation Plan

1. Add Spring Data JPA/Hibernate and local H2 dependencies; configure Hibernate schema validation.
2. Define the preference table and constraints in `schema.sql` for local development.
3. Map the entity and implement the Spring Data repository lookup; overlay stored rows on `true` defaults for GET.
4. Implement transactional service behavior and upserts for supplied PATCH fields.
5. Add controller validation, response mapping, and error handling; obtain `userId` from the established request context.
6. Run focused tests and the full Maven test suite; add a versioned migration when selecting a production database.

## Validation and Errors

- PATCH must contain at least one recognized field.
- Values must be JSON booleans; reject `null`, unknown fields, invalid JSON, and unsupported channel identifiers.
- `200 OK`: GET or successful PATCH (updated effective representation).
- `400 Bad Request`: invalid or empty PATCH.
- `401 Unauthorized`: no valid authentication; `403 Forbidden` if the authenticated caller is not authorized.
- `409 Conflict`: a preference write conflicts with an existing database row; retry the PATCH when appropriate.
- `415 Unsupported Media Type`: unsupported content type.
- No stored rows is not an error; return all channels as `true`.
- Unexpected persistence failure must roll back the whole PATCH and return a generic `500 Internal Server Error`; log diagnostic details server-side, not in the response.

## Testing

- **Service unit tests:** no rows returns all `true`; partial update changes only supplied channels; multiple fields are applied together; missing channels remain `true`.
- **Repository integration tests:** JPA lookup by user and database constraints for unique `(user_id, channel)`, allowed channel values, and non-null `enabled`.
- **Transaction test:** force a multi-channel write failure and verify no partial changes remain.
- **Controller/API tests:** GET/PATCH response shape, empty/malformed/missing PATCH bodies, unsupported content types, unknown fields/invalid values, centralized error statuses, and authenticated user scoping.
- **Concurrency check:** simultaneous writes to distinct channels preserve both; same-channel behavior is last committed value wins.
