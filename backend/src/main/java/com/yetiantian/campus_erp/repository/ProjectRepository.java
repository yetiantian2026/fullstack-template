package com.yetiantian.campus_erp.repository;
import com.yetiantian.campus_erp.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query(value = "SELECT COUNT(*) FROM project_member WHERE project_id = :projectId AND member_id = :memberId",
            nativeQuery = true)
    int countMembership(@Param("projectId") Long projectId, @Param("memberId") Long memberId);
}