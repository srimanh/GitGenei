package com.example.demo.repository;

import com.example.demo.entity.UploadedProject;
import com.example.demo.entity.UploadedProject.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedProjectRepository extends JpaRepository<UploadedProject, Long> {
    
    Optional<UploadedProject> findByFileId(String fileId);
    
    Optional<UploadedProject> findByFileIdAndUserId(String fileId, String userId);
    
    List<UploadedProject> findByUserId(String userId);
    
    List<UploadedProject> findByUserIdOrderByUploadTimeDesc(String userId);
    
    List<UploadedProject> findByStatus(ProjectStatus status);
    
    List<UploadedProject> findByStatusAndUserId(ProjectStatus status, String userId);
    
    @Query("SELECT p FROM UploadedProject p WHERE p.userId = :userId AND p.status = :status ORDER BY p.uploadTime DESC")
    List<UploadedProject> findByUserIdAndStatusOrderByUploadTimeDesc(@Param("userId") String userId, @Param("status") ProjectStatus status);
    
    @Query("SELECT p FROM UploadedProject p WHERE p.uploadTime < :cutoffTime AND p.status IN :statuses")
    List<UploadedProject> findOldProjectsByStatusIn(@Param("cutoffTime") LocalDateTime cutoffTime, @Param("statuses") List<ProjectStatus> statuses);
    
    @Query("SELECT COUNT(p) FROM UploadedProject p WHERE p.userId = :userId")
    Long countByUserId(@Param("userId") String userId);
    
    @Query("SELECT COUNT(p) FROM UploadedProject p WHERE p.userId = :userId AND p.status = :status")
    Long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") ProjectStatus status);
    
    @Query("SELECT SUM(p.fileSize) FROM UploadedProject p WHERE p.userId = :userId")
    Long getTotalFileSizeByUserId(@Param("userId") String userId);
    
    void deleteByFileId(String fileId);
    
    void deleteByFileIdAndUserId(String fileId, String userId);
    
    @Query("DELETE FROM UploadedProject p WHERE p.uploadTime < :cutoffTime")
    void deleteOldProjects(@Param("cutoffTime") LocalDateTime cutoffTime);
}
