package com.smellouk.autoguard.net

/**
 * Manual override controlled from the Quick Settings tile / app.
 *
 * - [AUTO]: normal behaviour — decide from the current network.
 * - [FORCE_ON]: keep the default tunnel(s) up everywhere, even on home Wi-Fi.
 * - [FORCE_OFF]: keep everything down, even on untrusted networks.
 */
enum class OverrideMode { AUTO, FORCE_ON, FORCE_OFF }
