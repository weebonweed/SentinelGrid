package com.sentinelgrid.service.interfaces;

import java.util.UUID;

public interface NotificationService {
    void notifyPostOwner(UUID postId, UUID actorId, String action);
    void processAllPending();
}
