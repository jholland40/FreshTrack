package com.example.freshtrack.core.model

/**
 * A lightweight profile mapping a Firebase Anonymous Auth uid to a
 * human-chosen display name. This does not replace anonymous sign-in -
 * every tester still gets a real, distinct uid - it just makes that uid
 * traceable to a person in the Firebase console and in Reports, instead of
 * a bare string like "086Kughn8gfwbFTm859FRF7C9m02".
 */
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
)
