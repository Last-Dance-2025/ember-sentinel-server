package com.inhacapstone04.embersentinelserver.fire_event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.LiveKitManagementService;
import com.inhacapstone04.embersentinelserver.media.entity.MediaStream;
import com.inhacapstone04.embersentinelserver.media.entity.StreamingStatus;
import com.inhacapstone04.embersentinelserver.media.repository.MediaStreamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FireEventWebhookService {

    private final MediaStreamRepository mediaStreamRepository;
    private final ObjectMapper objectMapper;
    private final LiveKitManagementService liveKitManagementService;

    /** Publisher의 비디오 트랙이 게시되면 스트리밍 상태를 LIVE로 변경합니다. */
    @Transactional
    public void processTrackPublished(String roomName, String participantIdentity) {
        if (roomName == null || roomName.isEmpty() || participantIdentity == null) return;

        MediaStream mediaStream = mediaStreamRepository.findByLivekitRoomName(roomName)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID,
                        "MediaStream not found for LiveKit room: " + roomName));
        Long cameraId = mediaStream.getFireEvent().getCameraEdge().getId();
        if (!("cam_" + cameraId).equals(participantIdentity)) {
            log.info("Ignoring track from non-publisher participant: {}", participantIdentity);
            return;
        }

        if (mediaStream.getStreamingStatus() == StreamingStatus.PENDING) {
            mediaStream.setStreamingStatus(StreamingStatus.LIVE);
            log.info("Streaming Status Updated to LIVE for FireEvent ID: {}", mediaStream.getFireEvent().getId());
        }
    }

    /**
     * [Webhook] 참여자 퇴장(participant_disconnected) 처리
     * Publisher(라즈베리파이)가 연결을 끊으면 DB 상태를 ENDED로 변경하고 LiveKit 방을 종료시킵니다.
     */
    @Transactional
    public void processParticipantDisconnected(String metadataJson, String roomName) {
        if (metadataJson == null || metadataJson.isEmpty()) return;

        try {
            JsonNode metadataNode = objectMapper.readTree(metadataJson);

            if (isPublisher(metadataNode)) {
                log.info("Publisher disconnected from room: {}. Ending fire event.", roomName);

                Long fireEventId = extractFireEventId(metadataNode);
                if (fireEventId == null) return;

                // 1. DB 상태 업데이트 (LIVE -> ENDED)
                MediaStream mediaStream = findMediaStream(fireEventId);

                if (mediaStream.getStreamingStatus() != StreamingStatus.ENDED) {
                    mediaStream.setStreamingStatus(StreamingStatus.ENDED);
                    log.info("Streaming Status Updated to ENDED for FireEvent ID: {}", fireEventId);
                }

                // 2. [위임] LiveKit Room 강제 삭제 요청 (Egress도 자동 종료됨)
                // DB 로직이 끝난 후 인프라 정리 요청
                liveKitManagementService.deleteRoom(roomName);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse participant metadata on disconnect", e);
        } catch (Exception e) {
            log.error("Error processing publisher disconnection", e);
        }
    }

    // --- Helper Methods ---

    private boolean isPublisher(JsonNode metadataNode) {
        return metadataNode.has("type") && "PUBLISHER".equals(metadataNode.get("type").asText());
    }

    private Long extractFireEventId(JsonNode metadataNode) {
        if (!metadataNode.has("fireEventId")) {
            log.warn("Publisher metadata missing fireEventId");
            return null;
        }
        return metadataNode.get("fireEventId").asLong();
    }

    private MediaStream findMediaStream(Long fireEventId) {
        return mediaStreamRepository.findByFireEvent_Id(fireEventId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID, "MediaStream not found for FireEvent ID: " + fireEventId));
    }
}
