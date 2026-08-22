package com.quantumos.quarkavatar

/*
 * The render paths QUARK actually ships on.
 *
 * The track evaluated five and the Director chose the NATIVE ART route (PRODUCTION_LOG Phase 14):
 * the hologram plates are shown as the art they are, with no reconstruction step anywhere in the
 * pipeline. Everything that reconstructed the character -- the Blender/MPFB model and its GLB, the
 * TripoSplat Gaussian splats, and the Phase 4b pre-rendered frames with their AGSL colour key --
 * has been removed rather than left switched off, because a dead render path is a thing future
 * sessions have to keep reading past.
 *
 * The DA3 relief went the same way in Phase 18, on the Director's call: its silhouette is one vertex
 * per source texel, so the outline is quantised into a staircase that Phase 17 measurably improved
 * (17.48 -> 6.54 px RMS) but could not bring to the flat plate's 3.12 without giving up the shared
 * textures that made it free. Slicing on a face is not a defect you ship. With it went SceneView,
 * Filament and tilt parallax.
 *
 * What is left is the art, shown as the art:
 *
 *   BODY   the full-figure plate. THE DEFAULT: a bust with no projection base reads as a head
 *          floating in a box, where the figure standing in the frame reads as a projection.
 *   BUST   the close plate. Kept because it is the tighter read for a conversation, and because
 *          both plate sets carry the same four expression states.
 */
enum class RenderMode(
    val label: String,
    /**
     * How much of the plate's height must run off the bottom edge to take its cut off screen, as a
     * fraction of the plate's own height.
     *
     * MEASURED per plate set off the matte, not shared, because the two cuts are shaped completely
     * differently -- and the BODY's is the reason this is not simply "the highest point of the cut".
     *
     *   BUST  NOT a scallop, and 0.17 was wrong. Re-measured in Phase 19: the bust plate is 98.8%
     *         OPAQUE through its bottom 15% at a mean luminance of ~190, and 99% of its columns run
     *         flush to the very edge. There is no matte cut in it at all. The "hard black scalloped
     *         curve" 0.17 was calibrated against belonged to the DA3 RELIEF's culled mesh boundary,
     *         and that path is gone -- so the figure was being cropped by ~12% of her height to hide
     *         a defect that no longer exists. All that has to leave the frame is the plate's own
     *         straight bottom edge: 0.05, with the CRT falloff dissolving the approach to it.
     *
     *   BODY  NOT a line at all. 88% of the columns run to within 0.05 of the plate's bottom edge;
     *         only the narrow notches beside and between the legs end higher, reaching 0.230. Sizing
     *         the overhang to that 0.230 was the first attempt and it is wrong: it buries the entire
     *         lower body -- thighs, hands, the whole reason for using the full figure -- to hide a
     *         few percent of the width. 0.08 clears the bulk, and the notches that remain sit inside
     *         the CRT falloff, which dissolves them.
     */
    val baseOverhang: Float,
    /**
     * Which FRAMING step this presentation opens at.
     *
     * Per mode, because the two plates are different shapes and the same width buys very different
     * heights. Measured on device: at 100% the BODY's head sits 29% down the frame and the BUST's
     * sits 49%, leaving the bust's upper half empty. The bust is squarer (0.815 against 0.567), so
     * it needs the larger step to fill the housing the way the body does at the smaller one.
     */
    val defaultFramingIndex: Int,
    /**
     * Fraction of the VIEWPORT height QUARK occupies at FRAMING 100% (C2 -- the fold).
     *
     * Why this exists: FRAMING sizes QUARK off the viewport's WIDTH, and these plates are much
     * taller than they are wide, so height is the dependent variable. That works on the one screen
     * this was ever tuned on -- 1080x2424 -- and breaks everywhere else. On the Fold 6's INNER
     * display (~2160x1856) the same width-driven maths asks for a BODY plate roughly **twice the
     * screen height**: her head leaves the frame entirely. Landscape does the same.
     *
     * These values are DERIVED, not chosen by eye: they are what the tuned phone screen already
     * produces at FRAMING 100% (BODY 0.786, BUST 0.547), rounded a hair UP so the clamp cannot bind
     * there through float equality. Two consequences worth stating: on the tuned screen nothing
     * changes at any reachable FRAMING setting, and on a wide screen QUARK is drawn at the SAME
     * PROPORTION the Director already approved rather than in a different composition.
     */
    val baseHeightFill: Float,
) {
    PLATE_BODY(
        "BODY (full figure, native art)",
        baseOverhang = 0.08f, defaultFramingIndex = 0, baseHeightFill = 0.80f,
    ),
    PLATE_BUST(
        "BUST (close, native art)",
        baseOverhang = 0.05f, defaultFramingIndex = 1, baseHeightFill = 0.56f,
    );
}
