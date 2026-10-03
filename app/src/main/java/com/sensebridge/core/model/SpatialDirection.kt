package com.sensebridge.core.model

/**
 * Spatial direction of an object or sound relative to the user.
 */
enum class SpatialDirection(val vietnameseLabel: String) {
    LEFT("bên trái"),
    CENTER("phía trước"),
    RIGHT("bên phải"),
    AHEAD("ngay trước mặt"),
    NEAR("ở gần"),
    FAR("ở xa"),
    UNKNOWN("");
}
