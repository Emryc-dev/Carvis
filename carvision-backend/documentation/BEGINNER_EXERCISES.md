# Beginner exercises

Use a separate practice folder/project and synthetic images. Each exercise has an objective, difficulty, concepts, instructions, expected output and optional challenge.

## Level 1 — Python

1. **Functions** — Objective: normalize a vehicle brand; difficulty: easy; concepts: parameters/returns. Write `normalize_brand(text)` that strips whitespace and title-cases it. Expected: `" bmw " -> "Bmw"`. Challenge: preserve `BMW` through an acronym map.
2. **Classes** — Objective: model a scan; easy; concepts: classes/dataclasses. Create a dataclass with ID, status and confidence. Expected: printable typed instance. Challenge: reject confidence outside 0–1.
3. **Dictionaries** — Objective: map AI JSON; easy; concepts: keys/defaults. Read optional engine with `.get`. Expected: missing engine yields `None`. Challenge: flatten nested evidence.
4. **Lists** — Objective: filter uncertain fields; easy; concepts: comprehensions. Keep non-null specification names. Expected: a cleaned list. Challenge: deduplicate while preserving order.
5. **Error handling** — Objective: handle invalid years; easy; concepts: `try/except`. Convert input to int and give a useful error. Expected: no crash on text. Challenge: custom exception.
6. **File processing** — Objective: hash an image; medium; concepts: binary I/O/chunks. Calculate SHA-256 without loading the whole file. Expected: 64-character hex. Challenge: compare duplicates.
7. **HTTP requests** — Objective: call `/health`; medium; concepts: timeout/status/JSON. Use HTTPX with a 3-second timeout. Expected: `{"status":"ok"}`. Challenge: retry only transient failures.

## Level 2 — FastAPI

1. **GET endpoint** — Objective: expose health; easy; concepts: routing. Return a status object. Expected: 200/OpenAPI entry. Challenge: add request ID.
2. **POST endpoint** — Objective: accept a vehicle; easy; concepts: methods/body. Echo a typed request. Expected: 201 JSON. Challenge: Location header.
3. **Request validation** — Objective: constrain year/confidence; medium; concepts: Pydantic fields. Expected: bad input returns 422. Challenge: cross-field validation.
4. **Response models** — Objective: prevent secret leakage; medium; concepts: serialization. Exclude internal notes from response. Expected: stable public JSON. Challenge: separate create/read schemas.
5. **Authentication dependency** — Objective: require bearer auth; medium; concepts: dependencies. Reject missing headers. Expected: 401. Challenge: override dependency in tests.
6. **Error handling** — Objective: standardize not-found; medium; concepts: exception handlers. Expected: code/message/request ID envelope. Challenge: map validation errors.

## Level 3 — SQLAlchemy

1. **Model** — Objective: map `Vehicle`; medium; concepts: declarative columns/constraints. Expected: metadata table. Challenge: composite identity index.
2. **Relationship** — Objective: link vehicle/specifications; medium; concepts: FK/one-to-many. Expected: eager-loaded specs. Challenge: cascade behavior.
3. **Insert** — Objective: persist one row; medium; concepts: session/commit/refresh. Expected: generated UUID. Challenge: transactional two-row insert.
4. **Query** — Objective: find by brand/model; medium; concepts: `select`. Expected: matching row. Challenge: case-insensitive search.
5. **Update** — Objective: change profile name; medium; concepts: unit of work. Expected: durable update. Challenge: optimistic version.
6. **Delete** — Objective: remove favorite; medium; concepts: delete/idempotency. Expected: absent association. Challenge: verify cascades.
7. **Join** — Objective: list a user's favorite vehicles; hard; concepts: joins/eager loading. Expected: one query-shaped result. Challenge: pagination count.

## Level 4 — PostgreSQL

1. **SELECT** — Objective: list newest scans; easy. Use `ORDER BY created_at DESC LIMIT 20`. Expected: newest first. Challenge: keyset pagination.
2. **INSERT** — Objective: add a profile; easy. Use UUID and JSON settings. Expected: one row. Challenge: `RETURNING`.
3. **UPDATE** — Objective: mark a scan completed; easy. Expected: status/timestamp updated. Challenge: update only from processing.
4. **DELETE** — Objective: remove one owned scan; easy. Include user predicate. Expected: no cross-user deletion. Challenge: inspect affected count.
5. **JOIN** — Objective: combine scan and vehicle identity; medium. Expected: nullable vehicle columns. Challenge: aggregate AI attempts.
6. **Indexes** — Objective: accelerate history; medium. Create `(user_id, created_at)`. Expected: plan uses index at scale. Challenge: compare `EXPLAIN ANALYZE`.
7. **Transactions** — Objective: atomically insert scan/AI request; hard. Force second statement failure. Expected: neither persists. Challenge: savepoint.

## Level 5 — Redis

1. **Store/retrieve** — Objective: set/get JSON; easy. Expected: same decoded object. Challenge: compact serialization.
2. **TTL** — Objective: expire a key; easy. Set 10 seconds and inspect TTL. Expected: key disappears. Challenge: jitter TTL.
3. **Cache** — Objective: implement cache-aside; medium. Count expensive calls. Expected: second request is a hit. Challenge: stampede lock.
4. **Invalidation** — Objective: remove stale vehicle data; medium. Delete after update. Expected: next read repopulates. Challenge: version keys.

## Level 6 — Image processing

1. **Open** — Objective: decode with Pillow; easy. Expected: format/size. Challenge: corrupt file.
2. **Resize** — Objective: fit within 1280×1280 while preserving aspect; medium. Expected: no distortion. Challenge: EXIF orientation.
3. **Metadata** — Objective: inspect format/mode/EXIF; easy. Expected: structured summary. Challenge: explain privacy risks.
4. **Convert** — Objective: PNG to JPEG; easy. Handle alpha. Expected: valid JPEG. Challenge: compare sizes/quality.
5. **Dimensions** — Objective: enforce min/max; easy. Expected: invalid sizes rejected. Challenge: decompression-bomb case.
6. **OpenCV basics** — Objective: grayscale and edge map; medium. Expected: two output arrays/images. Challenge: tune Canny thresholds. Install OpenCV only in this learning environment.

## Level 7 — AI

1. **Vision call** — Objective: send a test image through `AIService`; medium. Expected: typed identification. Challenge: record latency.
2. **Structured parsing** — Objective: parse provider JSON; medium. Expected: schema object. Challenge: reject extras.
3. **Output validation** — Objective: reject year 1500/confidence 4; medium. Expected: validation errors. Challenge: domain plausibility rules.
4. **Hallucination handling** — Objective: separate inferred and sourced facts; hard. Expected: no inferred “verified” spec. Challenge: provenance UI.
5. **Confidence logic** — Objective: bucket 0–1 confidence without claiming accuracy; medium. Expected: low/medium/high plus warning. Challenge: calibration plot from labeled data.

## Level 8 — ML foundations

1. **Arrays** — Objective: represent RGB pixels with NumPy; easy. Expected: `(height,width,3)` shape. Challenge: normalize channels.
2. **Tensors** — Objective: convert an array to a framework tensor; medium. Expected: batch/channel shape. Challenge: move to GPU if available.
3. **Datasets** — Objective: create labeled train/validation splits; medium. Expected: disjoint sets. Challenge: group by physical vehicle to prevent leakage.
4. **Training** — Objective: fit a tiny classifier on a tutorial dataset; hard. Expected: decreasing training loss. Challenge: augmentation.
5. **Validation** — Objective: calculate accuracy/confusion matrix on unseen data; hard. Expected: per-class failures visible. Challenge: top-k and calibration.
6. **Inference** — Objective: load fixed weights and predict one input; medium. Expected: label plus score. Challenge: latency benchmark and model card.
