package com.tonywww.deeprealm4th.astral;

/** How copies of the same filler ID behave inside one equipped container. */
public enum FillerActivation {
    /** A second copy cannot be inserted. Existing duplicates remain stored but inactive. */
    UNIQUE_WORN,
    /** Every copy contributes its rules. */
    STACKABLE,
    /** Multiple copies can be stored, but only the first open cell contributes rules. */
    UNIQUE_EFFECT
}
