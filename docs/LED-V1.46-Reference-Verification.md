# IZZ Radio V1.46 – reference font verification

## Master reference
- Uploaded original LED dot-matrix image: `392108.png` (259 × 180 PNG).
- Original SHA-256: `a3abafac6d2015365e02b172f2fd389aad0e74b17a4ab7e53ed8dc8e0aa04ca4`.
- The current V1.45 app screenshot is *not* used to derive the new glyphs.

## Image-measured source geometry
- Source glyphs sampled from the supplied image directly.
- **7 LED columns × 9 LED rows**, rather than the incorrectly assumed 7×8 in V1.45.
- Approximate measured source LED dot pitch: 2.75 pixels.
- Ten characters per row, five reference rows. 50 visible source slots including one blank.
- Source character grid measured and checked using a side-by-side source/cached-glyph comparison image.

## Validation scope
- A–Z and 0–9 are all measured from the uploaded original image.
- Visible symbols such as `+`, `-`, `×`, `÷`, `=`, `(`, `)`, `!`, `?`, `Σ`, `:`, and `.` are also referenced.
- Text metadata punctuation not depicted in the sample retains a compatible documented fallback; it is **not** asserted to be image-exact.
- Uncertainty due to antialiasing and limited source resolution: 4, D, N, X, space, colon, full stop, dash, divide, opening/closing parentheses, and exclamation mark initially had low automatic alignment margins. These were reviewed visually against the enlarged supplied source. Colon and full stop alignment were manually corrected from their visible LED dots.
- The measured array matches the sampled LED centers. This is not a mathematically proven 100% match to an unavailable uncompressed original bitmap; remaining antialiasing uncertainty must not be concealed.

## Guardrails
- Keep the existing Home Page 42dp-high banner slot and overall layout unchanged.
- Maintain Golden Amber for every segment.
- Preserve existing V1.43–V1.45 seamless right-to-left marquee at approximately 25dp per second.
- No new external audio, streaming or metadata dependencies.
- V1.46 TESTING uses a separate application ID, and must not be merged to Official without approval.
