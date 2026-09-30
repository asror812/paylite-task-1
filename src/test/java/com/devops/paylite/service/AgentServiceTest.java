package com.devops.paylite.service;

import static org.junit.jupiter.api.Assertions.*;

import com.devops.paylite.domain.Agent;
import com.devops.paylite.repository.AgentRepository;
import com.devops.paylite.security.SecurityUtils;
import com.devops.paylite.service.dto.TopUpRequestDto;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private AgentService agentService;

    @BeforeEach
    public void setUpAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("agent1", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetOwnBalance() {
        Mockito.when(agentRepository.findByLogin("agent1")).thenReturn(Optional.of(Agent.builder().login("agent1").balance(0L).build()));

        assertEquals("agent1", SecurityUtils.getCurrentUserLogin().get());
        assertEquals(0, agentRepository.findByLogin("agent1").get().getBalance());
    }

    @Test
    void shouldTopUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

        Agent agent = Agent.builder().id(1L).login("agent1").balance(0L).build();

        Mockito.when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
        agentService.topUp(1L, new TopUpRequestDto(100L));

        assertEquals(100L, agent.getBalance());
        Mockito.verify(agentRepository).save(Mockito.any(Agent.class));
    }
}
