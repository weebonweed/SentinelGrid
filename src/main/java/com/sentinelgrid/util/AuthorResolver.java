package com.sentinelgrid.util;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sentinelgrid.enums.AuthorType;
import com.sentinelgrid.exception.ResourceNotFoundException;
import com.sentinelgrid.repository.BotRepository;
import com.sentinelgrid.repository.UserRepository;

@Component
public class AuthorResolver {

    private final UserRepository userRepository;
    private final BotRepository botRepository;

    public AuthorResolver(UserRepository userRepository, BotRepository botRepository) {
        this.userRepository = userRepository;
        this.botRepository = botRepository;
    }

    public void assertExists(UUID authorId, AuthorType authorType) {
        boolean exists = switch (authorType) {
            case HUMAN -> userRepository.existsById(authorId);
            case BOT   -> botRepository.existsById(authorId);
        };
        if (!exists) {
            throw new ResourceNotFoundException(
                authorType.name() + " author not found: " + authorId);
        }
    }
}
