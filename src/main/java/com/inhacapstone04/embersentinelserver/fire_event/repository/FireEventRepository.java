package com.inhacapstone04.embersentinelserver.fire_event.repository;

import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FireEventRepository extends JpaRepository<FireEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fe FROM FireEvent fe WHERE fe.id = :id")
    Optional<FireEvent> findByIdForUpdate(@Param("id") Long id);

    // 카메라 ID를 기준으로 화재 이벤트를 페이징 조회 (최신순 정렬은 Pageable에서 처리)
    // N+1 방지를 위해 CameraEdge를 fetch join (alias 사용을 위해)
    @Query("SELECT fe FROM FireEvent fe JOIN FETCH fe.cameraEdge WHERE fe.cameraEdge.id = :cameraEdgeId")
    Page<FireEvent> findAllByCameraEdge_Id(@Param("cameraEdgeId") Long cameraEdgeId, Pageable pageable);
}
