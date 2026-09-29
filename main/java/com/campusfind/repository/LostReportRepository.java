package com.campusfind.repository;

import com.campusfind.entity.LostReport;
import com.campusfind.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LostReportRepository extends JpaRepository<LostReport, Long> {

    List<LostReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<LostReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    long countByStatus(ReportStatus status);

    boolean existsByCategoryId(Long categoryId);

    List<LostReport> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT r FROM LostReport r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:categoryId IS NULL OR r.category.id = :categoryId) AND " +
           "(:location IS NULL OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:query IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY r.createdAt DESC")
    List<LostReport> searchReports(
            @Param("status") ReportStatus status,
            @Param("categoryId") Long categoryId,
            @Param("location") String location,
            @Param("query") String query
    );
}
