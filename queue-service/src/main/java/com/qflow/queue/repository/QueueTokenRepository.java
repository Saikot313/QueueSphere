package com.qflow.queue.repository;

import com.qflow.queue.entity.QueueToken;
import com.qflow.queue.entity.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QueueTokenRepository extends JpaRepository<QueueToken, Long> {

    Optional<QueueToken> findByIdAndTenantId(Long id, Long tenantId);

    List<QueueToken> findByStatusAndTokenDate(TokenStatus status, LocalDate date);

    Optional<QueueToken> findFirstByQueueIdAndTokenDateAndStatusInOrderByCalledAtDesc(
            Long queueId, LocalDate date, Collection<TokenStatus> statuses);

    @Query("select coalesce(max(t.tokenNumber), 0) from QueueToken t where t.queueId = :queueId and t.tokenDate = :date")
    int findMaxTokenNumber(@Param("queueId") Long queueId, @Param("date") LocalDate date);
}