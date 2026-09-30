package com.devops.paylite.service;

import com.devops.paylite.config.PaymentProperties;
import com.devops.paylite.domain.Agent;
import com.devops.paylite.domain.Payment;
import com.devops.paylite.domain.enumeration.PaymentStatus;
import com.devops.paylite.repository.AgentRepository;
import com.devops.paylite.repository.PaymentRepository;
import com.devops.paylite.security.SecurityUtils;
import com.devops.paylite.service.dto.PaymentCreateRequestDto;
import com.devops.paylite.service.dto.PaymentDTO;
import com.devops.paylite.service.mapper.PaymentMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service Implementation for managing {@link com.devops.paylite.domain.Payment}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final AgentRepository agentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentProperties paymentProperties;

    /**
     * Save a payment.
     *
     * @param dto the entity to save.
     * @return the persisted entity.
     */
    @Transactional
    public PaymentDTO save(PaymentCreateRequestDto dto) {
        LOG.debug("Request to save Payment : {}", dto);

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        Agent agent = agentRepository
            .findByLogin(login)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        long commissionAmount = BigDecimal.valueOf(dto.getAmount())
            .multiply(paymentProperties.getCommissionPercent())
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .longValueExact();

        long totalAmount = dto.getAmount() + commissionAmount;

        PaymentStatus status = agent.getBalance() >= totalAmount ? PaymentStatus.PAID : PaymentStatus.FAILED;

        Payment payment = Payment.builder()
            .account(dto.getAccount())
            .amount(dto.getAmount())
            .totalAmount(totalAmount)
            .commissionAmount(commissionAmount)
            .status(status)
            .agent(agent)
            .createdDate(Instant.now())
            .build();

        if (status == PaymentStatus.PAID) {
            agent.setBalance(agent.getBalance() - totalAmount);
            agentRepository.save(agent);
        } else {
            LOG.info("Insufficient balance for agent: {}", agent.getLogin());
        }

        payment = paymentRepository.save(payment);
        return paymentMapper.toDto(payment);
    }

    /**
     * Update a payment.
     *
     * @param paymentDTO the entity to save.
     * @return the persisted entity.
     */
    @Transactional
    public PaymentDTO update(PaymentDTO paymentDTO) {
        LOG.debug("Request to update Payment : {}", paymentDTO);
        Payment payment = paymentMapper.toEntity(paymentDTO);
        payment = paymentRepository.save(payment);
        return paymentMapper.toDto(payment);
    }

    /**
     * Partially update a payment.
     *
     * @param paymentDTO the entity to update partially.
     * @return the persisted entity.
     */
    @Transactional
    public Optional<PaymentDTO> partialUpdate(PaymentDTO paymentDTO) {
        LOG.debug("Request to partially update Payment : {}", paymentDTO);

        return paymentRepository
            .findById(paymentDTO.getId())
            .map(existingPayment -> {
                paymentMapper.partialUpdate(existingPayment, paymentDTO);

                return existingPayment;
            })
            .map(paymentRepository::save)
            .map(paymentMapper::toDto);
    }

    /**
     * Get all the payments.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<PaymentDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Payments");
        return paymentRepository.findAll(pageable).map(paymentMapper::toDto);
    }

    public Page<PaymentDTO> getOwn(Pageable pageable) {
        LOG.debug("Request to get all Own Payments");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        Agent agent = agentRepository
            .findByLogin(login)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        return paymentRepository.findAllByAgent_Login(agent.getLogin(), pageable).map(paymentMapper::toDto);
    }

    /**
     * Get one payment by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<PaymentDTO> findOne(Long id) {
        LOG.debug("Request to get Payment : {}", id);

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user login not found"));

        Agent agent = agentRepository
            .findByLogin(login)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        if (!paymentRepository.findByIdAndAgent_Login(id, agent.getLogin()).isPresent()) {}
        return paymentRepository.findById(id).map(paymentMapper::toDto);
    }

    /**
     * Delete the payment by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Payment : {}", id);
        paymentRepository.deleteById(id);
    }
}
