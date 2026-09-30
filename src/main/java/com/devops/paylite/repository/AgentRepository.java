package com.devops.paylite.repository;

import com.devops.paylite.domain.Agent;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Agent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByLogin(String login);
}
