package com.quantumos.quarkavatar

/*
 * Local to this dev-preview screen -- deliberately NOT com.quantumos.core.QuarkReflexPosture
 * (IDLE/SCAN/HAPPY/WARN), which is the abstract QuarkMascot's own reflex-animation state model, a
 * different thing than the avatar's posture library. Mirrors the three bundled bakes: NEUTRAL and
 * THINKING carry the live-retinted green accent; ALERT is pre-baked fixed --warn red and is never
 * retinted (see QuarkAvatarScreen's accentIsLive gate).
 */
enum class DemoPosture {
    NEUTRAL,
    ALERT,
    THINKING
}
