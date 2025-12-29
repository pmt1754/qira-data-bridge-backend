package com.app.repository;

import com.app.model.IssueRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// @Repository - Disabled until database is configured
public interface IssueRecordRepository extends JpaRepository<IssueRecord, Long> {
    
    Optional<IssueRecord> findByQiraId(String qiraId);
    
    @Query("SELECT i.qiraId FROM IssueRecord i WHERE i.qiraId IN :qiraIds")
    Set<String> findExistingQiraIds(@Param("qiraIds") Set<String> qiraIds);
    
    @Query("SELECT i FROM IssueRecord i WHERE i.createdAt >= :fromDate AND i.createdAt <= :toDate")
    List<IssueRecord> findByDateRange(
        @Param("fromDate") OffsetDateTime fromDate,
        @Param("toDate") OffsetDateTime toDate
    );
    
    @Query("SELECT i FROM IssueRecord i WHERE i.createdAt >= :fromDate AND i.createdAt <= :toDate " +
           "AND (:team IS NULL OR i.assignedTeam = :team)")
    List<IssueRecord> findByDateRangeAndTeam(
        @Param("fromDate") OffsetDateTime fromDate,
        @Param("toDate") OffsetDateTime toDate,
        @Param("team") String team
    );
    
    @Query("SELECT COUNT(i) FROM IssueRecord i WHERE i.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT i.issueType as type, COUNT(i) as count FROM IssueRecord i " +
           "GROUP BY i.issueType ORDER BY count DESC")
    List<Object[]> countByIssueType();
}
