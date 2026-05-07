package com.sentinelgrid.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sentinelgrid.entity.Bot;

@Repository
public interface BotRepository extends JpaRepository<Bot, UUID> {
}
