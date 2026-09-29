package com.campusfind.repository;

import com.campusfind.entity.FoundItem;
import com.campusfind.entity.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FoundItemRepository extends JpaRepository<FoundItem, Long> {

    List<FoundItem> findByStatusOrderByCreatedAtDesc(ItemStatus status);

    List<FoundItem> findByStatusInOrderByCreatedAtDesc(Collection<ItemStatus> statuses);

    List<FoundItem> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    long countByStatus(ItemStatus status);

    boolean existsByCategoryId(Long categoryId);

    List<FoundItem> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT f FROM FoundItem f WHERE " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:categoryId IS NULL OR f.category.id = :categoryId) AND " +
           "(:location IS NULL OR LOWER(f.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:query IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY f.createdAt DESC")
    List<FoundItem> searchItems(
            @Param("status") ItemStatus status,
            @Param("categoryId") Long categoryId,
            @Param("location") String location,
            @Param("query") String query
    );
}
