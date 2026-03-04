package com.gokarting.domain.exception;

import java.util.UUID;

public class SlotFullException extends RuntimeException {
    public SlotFullException(UUID slotId) {
        super("Time slot %s has no remaining capacity".formatted(slotId));
    }
}
