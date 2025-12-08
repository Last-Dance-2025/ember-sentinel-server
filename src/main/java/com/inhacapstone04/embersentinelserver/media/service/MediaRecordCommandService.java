package com.inhacapstone04.embersentinelserver.media.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.fire_event.entity.FireEvent;
import com.inhacapstone04.embersentinelserver.fire_event.repository.FireEventRepository;
import com.inhacapstone04.embersentinelserver.media.entity.MediaRecord;
import com.inhacapstone04.embersentinelserver.media.repository.MediaRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaRecordCommandService {

    private final MediaRecordRepository mediaRecordRepository;
    private final FireEventRepository fireEventRepository;

    /**
     * Egress(녹화) 완료 후 전달받은 S3 경로와 방 이름을 기반으로 녹화 정보를 저장합니다.
     *
     * @param roomName LiveKit 방 이름 (예: "fire_event_101")
     * @param s3Url    업로드된 S3 파일 URL
     */
    @Transactional
    public void saveRecording(String roomName, String s3Url) {
        // 1. 방 이름에서 Event ID 추출
        // 규칙: "fire_event_{id}" 형식
        Long eventId = parseIdFromRoomName(roomName);

        if (eventId == null) {
            log.error("Failed to parse FireEvent ID from room name: {}", roomName);
            // Webhook 처리는 200 OK를 반환해야 하므로 예외를 던지지 않고 로깅만 하고 종료하거나,
            // 필요에 따라 CustomException을 던져서 재시도를 유도할 수 있습니다.
            // 여기서는 데이터 정합성이 깨진 상황이므로 로깅 후 종료합니다.
            return;
        }

        // 2. FireEvent 조회
        FireEvent fireEvent = fireEventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND_BY_ID,
                        "MediaRecord 저장 중 FireEvent를 찾을 수 없습니다. ID: " + eventId
                ));

        if (mediaRecordRepository.existsByFireEvent_Id(eventId)) {
            log.info("MediaRecord already exists for FireEvent ID: {}. Ignoring duplicate webhook.", eventId);
            return;
        }

        // 3. MediaRecord 엔티티 생성 및 저장
        MediaRecord mediaRecord = new MediaRecord();
        mediaRecord.setFireEvent(fireEvent);
        mediaRecord.setS3BucketPath(s3Url);

        // 필요하다면 파일 크기, 녹화 시간 등의 메타데이터도 여기서 설정 가능 (Webhook 데이터에 포함되어 있다면)

        mediaRecordRepository.save(mediaRecord);
        log.info("MediaRecord saved successfully. ID: {}, EventID: {}, URL: {}",
                mediaRecord.getId(), eventId, s3Url);
    }

    /**
     * 방 이름 문자열에서 ID 부분만 추출하여 Long으로 변환합니다.
     */
    private Long parseIdFromRoomName(String roomName) {
        try {
            // "fire_event_" 접두사 제거
            if (roomName.startsWith("fire_event_")) {
                String idPart = roomName.replace("fire_event_", "");
                return Long.parseLong(idPart);
            }
            return null;
        } catch (NumberFormatException e) {
            log.error("Invalid number format in room name: {}", roomName, e);
            return null;
        }
    }
}
