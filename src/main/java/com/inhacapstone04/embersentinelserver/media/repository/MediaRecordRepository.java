package com.inhacapstone04.embersentinelserver.media.repository;

import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRecordRepository extends JpaRepository<MediaRecord, Long> {
    boolean existsByFireEvent_Id(Long fireEventId);
}
