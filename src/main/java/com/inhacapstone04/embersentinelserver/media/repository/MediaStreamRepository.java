package com.inhacapstone04.embersentinelserver.media.repository;

import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaStreamRepository extends JpaRepository<MediaStream, Long> {
    Optional<MediaStream> findByFireEvent_Id(Long fireEventId);
    Optional<MediaStream> findByLivekitRoomName(String livekitRoomName);
}
