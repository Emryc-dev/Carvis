# AI guide

Multimodal models combine language and visual representations, but image classification here is probabilistic inference—not VIN decoding or proof of trim. Similar generations, aftermarket parts, occlusion and regional variants create ambiguity. A high model confidence is not calibrated real-world accuracy.

Gemini receives image bytes plus a conservative prompt and JSON schema. Temperature is low, but structured output still needs Pydantic validation. Unknown specifications must be null; visual evidence and uncertainties explain the result. The backend never converts inferred output into a verified specification. Verification requires a named reliable source and, ideally, identifiers unavailable from appearance alone.

Hallucination is plausible-looking unsupported output. Reduce it with narrow prompts, explicit nullability, schema constraints, catalog matching, provenance, range checks, evaluation datasets and human review—not by claiming certainty. Measure accuracy by make/model/year separately on a held-out, diverse, licensed dataset; inspect calibration and subgroup failures. Log model/version/latency/error without retaining more personal imagery than necessary.

The `AIService` contract permits future OpenAI support. Provider implementations translate their API response into the same domain schema; routes and database code remain unchanged.
