package com.messledger.app.data

/**
 * How the journal chooses its light or night paper.
 *
 * [SYSTEM] follows the phone until the reader taps the masthead switch. The
 * first tap locks the opposite mode, and a long press returns here.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}
