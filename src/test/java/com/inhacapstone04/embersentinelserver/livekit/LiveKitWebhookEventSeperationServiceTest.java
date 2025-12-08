package com.inhacapstone04.embersentinelserver.livekit;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.LiveKitWebhookEventSeperationService;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventWebhookService;
import com.inhacapstone04.embersentinelserver.media.service.MediaRecordCommandService;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import livekit.LivekitWebhook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveKitWebhookEventSeperationServiceTest {

    @InjectMocks
    private LiveKitWebhookEventSeperationService seperationService;

    @Mock
    private WebhookReceiver webhookReceiver;

    @Mock
    private MediaRecordCommandService mediaRecordCommandService;

    @Mock
    private FireEventWebhookService fireEventWebhookService;

    @Test
    @DisplayName("Egress 종료 이벤트(성공) - Filename(Key)이 저장되어야 한다")
    void handleWebhookEvent_EgressEnded_Success() {
        // Given
        String body = "dummy_body";
        String authHeader = "dummy_auth";
        String roomName = "fire_event_123";
        String s3Key = "recordings/test_video.mp4"; // 저장되어야 할 Key
        String s3Location = "https://s3.aws.com/recordings/test_video.mp4"; // 로그용 URL

        // Mock 객체 생성 (LiveKit Protobuf 객체)
        LivekitWebhook.WebhookEvent mockEvent = mock(LivekitWebhook.WebhookEvent.class);
        LivekitEgress.EgressInfo mockEgressInfo = mock(LivekitEgress.EgressInfo.class);
        LivekitEgress.FileInfo mockFileInfo = mock(LivekitEgress.FileInfo.class);

        // 1. Webhook 수신 Mocking
        given(webhookReceiver.receive(body, authHeader)).willReturn(mockEvent);
        given(mockEvent.getEvent()).willReturn("egress_ended");

        // 2. Egress 정보 Mocking
        given(mockEvent.hasEgressInfo()).willReturn(true);
        given(mockEvent.getEgressInfo()).willReturn(mockEgressInfo);

        // 3. Egress 상세 정보 Mocking
        given(mockEgressInfo.getRoomName()).willReturn(roomName);
        given(mockEgressInfo.getEgressId()).willReturn("EG_12345");
        given(mockEgressInfo.getStatus()).willReturn(LivekitEgress.EgressStatus.EGRESS_COMPLETE); // 성공 상태

        // 4. File 정보 Mocking
        given(mockEgressInfo.hasFile()).willReturn(true);
        given(mockEgressInfo.getFile()).willReturn(mockFileInfo);

        // [핵심] getFilename()이 Key를 반환하도록 설정
        given(mockFileInfo.getFilename()).willReturn(s3Key);
        // 로그 로직 방어용 (getLocation 호출 시 null이 아니도록)
        given(mockFileInfo.getLocation()).willReturn(s3Location);

        // When
        seperationService.handleWebhookEvent(body, authHeader);

        // Then
        // [검증] saveRecording 메서드가 's3Location'이 아니라 's3Key'로 호출되었는지 확인
        verify(mediaRecordCommandService, times(1)).saveRecording(roomName, s3Key);
    }

    @Test
    @DisplayName("Egress 종료 이벤트(실패) - 상태가 COMPLETE가 아니면 저장하지 않는다")
    void handleWebhookEvent_EgressEnded_Failed() {
        // Given
        String body = "dummy_body";
        String authHeader = "dummy_auth";

        LivekitWebhook.WebhookEvent mockEvent = mock(LivekitWebhook.WebhookEvent.class);
        LivekitEgress.EgressInfo mockEgressInfo = mock(LivekitEgress.EgressInfo.class);

        given(webhookReceiver.receive(body, authHeader)).willReturn(mockEvent);
        given(mockEvent.getEvent()).willReturn("egress_ended");

        given(mockEvent.hasEgressInfo()).willReturn(true);
        given(mockEvent.getEgressInfo()).willReturn(mockEgressInfo);

        // 상태가 FAILED 임
        given(mockEgressInfo.getStatus()).willReturn(LivekitEgress.EgressStatus.EGRESS_FAILED);
        given(mockEgressInfo.getError()).willReturn("Unknown Error"); // 로그용

        // When
        seperationService.handleWebhookEvent(body, authHeader);

        // Then
        // 저장 로직이 호출되지 않아야 함
        verify(mediaRecordCommandService, never()).saveRecording(anyString(), anyString());
    }

    @Test
    @DisplayName("카메라 비디오 트랙 게시 이벤트를 전달한다")
    void handleWebhookEvent_VideoTrackPublished() {
        String body = "body";
        String authHeader = "auth";
        LivekitWebhook.WebhookEvent mockEvent = mock(LivekitWebhook.WebhookEvent.class);
        LivekitModels.ParticipantInfo mockParticipant = mock(LivekitModels.ParticipantInfo.class);
        LivekitModels.Room mockRoom = mock(LivekitModels.Room.class);
        LivekitModels.TrackInfo mockTrack = mock(LivekitModels.TrackInfo.class);

        given(webhookReceiver.receive(body, authHeader)).willReturn(mockEvent);
        given(mockEvent.getEvent()).willReturn("track_published");
        given(mockEvent.hasRoom()).willReturn(true);
        given(mockEvent.hasParticipant()).willReturn(true);
        given(mockEvent.hasTrack()).willReturn(true);
        given(mockEvent.getRoom()).willReturn(mockRoom);
        given(mockEvent.getParticipant()).willReturn(mockParticipant);
        given(mockEvent.getTrack()).willReturn(mockTrack);
        given(mockRoom.getName()).willReturn("fire_event_101");
        given(mockParticipant.getIdentity()).willReturn("cam_7");
        given(mockTrack.getType()).willReturn(LivekitModels.TrackType.VIDEO);

        seperationService.handleWebhookEvent(body, authHeader);

        verify(fireEventWebhookService).processTrackPublished("fire_event_101", "cam_7");
        verify(mockParticipant, never()).getMetadata();
    }

    @Test
    @DisplayName("오디오 트랙 게시로는 LIVE 전환을 요청하지 않는다")
    void handleWebhookEvent_AudioTrackPublished() {
        LivekitWebhook.WebhookEvent mockEvent = mock(LivekitWebhook.WebhookEvent.class);
        LivekitModels.TrackInfo mockTrack = mock(LivekitModels.TrackInfo.class);
        given(webhookReceiver.receive("body", "auth")).willReturn(mockEvent);
        given(mockEvent.getEvent()).willReturn("track_published");
        given(mockEvent.hasRoom()).willReturn(true);
        given(mockEvent.hasParticipant()).willReturn(true);
        given(mockEvent.hasTrack()).willReturn(true);
        given(mockEvent.getTrack()).willReturn(mockTrack);
        given(mockTrack.getType()).willReturn(LivekitModels.TrackType.AUDIO);

        seperationService.handleWebhookEvent("body", "auth");

        verifyNoInteractions(fireEventWebhookService);
    }

    @Test
    @DisplayName("참여자 입장만으로는 LIVE 전환을 요청하지 않는다")
    void handleWebhookEvent_ParticipantJoinedIgnored() {
        LivekitWebhook.WebhookEvent mockEvent = mock(LivekitWebhook.WebhookEvent.class);
        given(webhookReceiver.receive("body", "auth")).willReturn(mockEvent);
        given(mockEvent.getEvent()).willReturn("participant_joined");

        seperationService.handleWebhookEvent("body", "auth");

        verifyNoInteractions(fireEventWebhookService);
    }

    @Test
    @DisplayName("Participant Disconnected 이벤트 - Metadata와 RoomName으로 처리한다")
    void handleWebhookEvent_ParticipantDisconnected() {
        // Given
        String body = "body";
        String authHeader = "auth";
        String metadata = "{\"userId\":1}";
        String roomName = "room_1";

        LivekitWebhook.WebhookEvent mockEvent = mock(LivekitWebhook.WebhookEvent.class);
        livekit.LivekitModels.ParticipantInfo mockParticipant = mock(livekit.LivekitModels.ParticipantInfo.class);
        livekit.LivekitModels.Room mockRoom = mock(livekit.LivekitModels.Room.class);

        given(webhookReceiver.receive(body, authHeader)).willReturn(mockEvent);
        given(mockEvent.getEvent()).willReturn("participant_disconnected");

        given(mockEvent.hasParticipant()).willReturn(true);
        given(mockEvent.getParticipant()).willReturn(mockParticipant);
        given(mockParticipant.getMetadata()).willReturn(metadata);

        given(mockEvent.getRoom()).willReturn(mockRoom);
        given(mockRoom.getName()).willReturn(roomName);

        // When
        seperationService.handleWebhookEvent(body, authHeader);

        // Then
        verify(fireEventWebhookService, times(1)).processParticipantDisconnected(metadata, roomName);
    }

    @Test
    @DisplayName("웹훅 검증 실패 시 CustomException을 던진다")
    void handleWebhookEvent_ValidationFail() {
        // Given
        String body = "invalid_body";
        String authHeader = "invalid_auth";

        // receive 메서드 호출 시 예외 발생하도록 설정
        doThrow(new RuntimeException("Signature validation failed"))
                .when(webhookReceiver).receive(body, authHeader);

        // When & Then
        assertThrows(CustomException.class, () ->
                seperationService.handleWebhookEvent(body, authHeader)
        );
    }
}
