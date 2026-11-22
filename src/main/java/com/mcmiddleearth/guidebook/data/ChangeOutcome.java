package com.mcmiddleearth.guidebook.data;

import java.io.IOException;

/**
 * How a change to an Area through the {@link AreaStore} ended. A step returns only some of these, and its caller can
 * ignore the rest.
 */
public sealed interface ChangeOutcome {

    /** The change was written and made. */
    record Done() implements ChangeOutcome {}

    /** The Area was deleted or reloaded meanwhile, so it isn't the one the store holds, and nothing was changed. */
    record Gone() implements ChangeOutcome {}

    /** Another Area took the name meanwhile, so nothing was changed. */
    record NameTaken() implements ChangeOutcome {}

    /** The Area's file couldn't be written, or deleted, so the Area was left as it was. */
    record NotWritten(IOException cause) implements ChangeOutcome {}

    /**
     * The change was written and made, but the Area's old file, in {@code worldName}'s folder under {@code areaName},
     * couldn't be deleted, so the Area would load twice after a reload.
     */
    record DoneButOldFileLeft(String worldName, String areaName) implements ChangeOutcome {}
}
