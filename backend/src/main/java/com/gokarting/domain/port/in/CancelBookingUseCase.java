package com.gokarting.domain.port.in;

import java.util.UUID;

public interface CancelBookingUseCase {

    record CancelCommand(UUID bookingId, UUID requestingUserId) {}

    void cancel(CancelCommand command);
}
