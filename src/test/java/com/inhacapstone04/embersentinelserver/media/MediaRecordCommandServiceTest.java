package com.inhacapstone04.embersentinelserver.media;

import com.inhacapstone04.embersentinelserver.building.entity.Building;
import com.inhacapstone04.embersentinelserver.building.repository.BuildingRepository;
import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.fire_event.entity.DetectionType;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireCause;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;
import com.inhacapstone04.embersentinelserver.media.repository.MediaRecordRepository;
import com.inhacapstone04.embersentinelserver.media.service.MediaRecordCommandService;
import com.inhacapstone04.embersentinelserver.room.entity.Room;
import com.inhacapstone04.embersentinelserver.room.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("MediaRecordCommandService 통합 테스트")
class MediaRecordCommandServiceTest {

    @Autowired
    private MediaRecordCommandService mediaRecordCommandService;

    @Autowired
    private MediaRecordRepository mediaRecordRepository;
    @Autowired private FireEventRepository fireEventRepository;
    @Autowired private CameraEdgeRepository cameraEdgeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BuildingRepository buildingRepository;

    private FireEvent savedFireEvent;

    @BeforeEach
    void setUp() {
        // 테스트를 위한 기초 데이터 생성 (Building -> Room -> CameraEdge -> FireEvent)
        Building building = new Building();
        building.setBuildingName("Test Building");
        buildingRepository.save(building);

        Room room = new Room();
        room.setRoomAlias("Test Room");
        room.setBuilding(building);
        roomRepository.save(room);

        CameraEdge camera = new CameraEdge();
        camera.setRoom(room);
        camera.setDeviceUuid("cam-uuid-1234");
        camera.setCameraEdgeAlias("Test Cam");
        cameraEdgeRepository.save(camera);

        FireEvent fireEvent = new FireEvent();
        fireEvent.setCameraEdge(camera);
        fireEvent.setDetectionType(DetectionType.FIRE);
        fireEvent.setFireCause(FireCause.기타);
        fireEvent.setRiskRank(0L);
        savedFireEvent = fireEventRepository.save(fireEvent);
    }

    @Test
    @DisplayName("성공: 정상적인 방 이름과 존재하는 이벤트 ID로 녹화 파일 저장")
    void saveRecording_Success() {
        // Given
        Long eventId = savedFireEvent.getId();
        String validRoomName = "fire_event_" + eventId;
        String s3Url = "s3://bucket/recordings/" + validRoomName + ".mp4";

        // When
        mediaRecordCommandService.saveRecording(validRoomName, s3Url);

        // Then
        // 1. 해당 이벤트에 연결된 MediaRecord가 저장되었는지 확인
        List<MediaRecord> records = mediaRecordRepository.findAll();
        assertThat(records).hasSize(1);

        MediaRecord savedRecord = records.get(0);
        assertThat(savedRecord.getFireEvent().getId()).isEqualTo(eventId);
        assertThat(savedRecord.getS3BucketPath()).isEqualTo(s3Url);
    }

    @Test
    @DisplayName("같은 녹화 웹훅이 재전송되어도 MediaRecord는 하나만 저장한다")
    void saveRecording_DuplicateWebhook() {
        String roomName = "fire_event_" + savedFireEvent.getId();
        String s3Url = "recordings/" + roomName + ".mp4";

        mediaRecordCommandService.saveRecording(roomName, s3Url);
        mediaRecordCommandService.saveRecording(roomName, s3Url);

        assertThat(mediaRecordRepository.findAll())
                .singleElement()
                .satisfies(record -> assertThat(record.getS3BucketPath()).isEqualTo(s3Url));
    }

    @Test
    @DisplayName("실패(무시): 방 이름 포맷이 올바르지 않은 경우 (저장하지 않고 종료)")
    void saveRecording_Fail_InvalidFormat() {
        // Given
        String invalidRoomName = "random_room_name"; // "fire_event_" 접두사가 없음
        String s3Url = "s3://bucket/test.mp4";

        long countBefore = mediaRecordRepository.count();

        // When
        // 예외를 던지지 않고 로그만 남기고 종료되어야 함
        mediaRecordCommandService.saveRecording(invalidRoomName, s3Url);

        // Then
        long countAfter = mediaRecordRepository.count();
        assertThat(countAfter).isEqualTo(countBefore); // 저장된 데이터가 없어야 함
    }

    @Test
    @DisplayName("실패(무시): 방 이름 포맷은 맞지만 ID가 숫자가 아닌 경우")
    void saveRecording_Fail_InvalidIdFormat() {
        // Given
        String invalidIdRoomName = "fire_event_abc"; // 숫자가 아님
        String s3Url = "s3://bucket/test.mp4";

        long countBefore = mediaRecordRepository.count();

        // When
        mediaRecordCommandService.saveRecording(invalidIdRoomName, s3Url);

        // Then
        long countAfter = mediaRecordRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("실패(예외): 방 이름 포맷은 맞지만 해당 ID의 FireEvent가 없는 경우 (404)")
    void saveRecording_Fail_EventNotFound() {
        // Given
        Long nonExistentId = 9999L;
        String roomName = "fire_event_" + nonExistentId;
        String s3Url = "s3://bucket/test.mp4";

        // When & Then
        assertThatThrownBy(() -> mediaRecordCommandService.saveRecording(roomName, s3Url))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND_BY_ID);
    }
}
