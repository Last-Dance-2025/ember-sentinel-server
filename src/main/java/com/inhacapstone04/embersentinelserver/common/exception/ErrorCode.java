package com.inhacapstone04.embersentinelserver.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    // auth
    INVALID_INPUT_VALUE(HttpStatus.FORBIDDEN, "지원하지 않 소셜 로그인 타입입니다."),
    USER_ALREADY_SIGN_OUT(HttpStatus.UNAUTHORIZED, "로그아웃 되었습니다."),
    EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 필요한 경로에 빈 토큰으로 요청했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증이 필요한 접근입니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "액세스 토큰이 만료되었습니다. 재발급 api를 호출해주세요."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다. 다시 로그인을 진행해주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    REFRESH_TOKEN_NOT_EXISTS(HttpStatus.BAD_REQUEST, "리프레시 토큰이 존재하지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    PASSWORD_NOT_CHANGED(HttpStatus.BAD_REQUEST, "현재 비밀번호와 새 비밀번호가 동일합니다."),
    PASSWORD_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "새 비밀번호가 일치하지 않습니다."),
    SIGN_IN_FAILED(HttpStatus.UNAUTHORIZED, "로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요."),

    // s3
    S3_SERVICE_EXCEPTION(HttpStatus.BAD_REQUEST, "S3 서비스 에러 발생"),
    S3_CLIENT_EXCEPTION(HttpStatus.BAD_REQUEST, "S3 클라이언트 에러 발생"),
    FILE_NOT_EXIST(HttpStatus.BAD_REQUEST, "파일이 없습니다."),
    NOT_ALLOWED_FILE_EXTENSIONS(HttpStatus.BAD_REQUEST, "허용되지 않은 확장자입니다."),
    INVALID_FILE_EXTENSIONS(HttpStatus.BAD_REQUEST, "파일 형식이 유효하지 않습니다."),

    // service
    NOT_FOUND_BY_ID(HttpStatus.NOT_FOUND, "해당 ID로 존재하는 정보가 없습니다."),
    NOT_AUTHORIZED_ACCESS_BY_ID(HttpStatus.FORBIDDEN, "권한이 없는 리소스에 접근중이거나, 잘못된 리소스 ID를 사용해 접근중입니다."),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 ID로 존재하는 사용자 정보가 없습니다."),
    NOT_FOUND_BY_EMAIL(HttpStatus.NOT_FOUND, "해당 이메일로 존재하는 사용자 정보가 없습니다."),
    ALREADY_MEMBER_OF_ROOM(HttpStatus.CONFLICT, "해당 사용자는 이미 Room에 등록된 사용자입니다."),
    CANNOT_REMOVE_SELF(HttpStatus.BAD_REQUEST, "자신에 대한 정보를 스스로 삭제할 수 없습니다."),
    LIVEKIT_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "LiveKit 서버와 통신 중 오류가 발생했습니다."),
    STREAM_NOT_LIVE(HttpStatus.CONFLICT, "현재 실시간 스트리밍 중인 이벤트가 아닙니다."),
    DUPLICATE_DEVICE_UUID(HttpStatus.CONFLICT, "이미 존재하는 device UUID입니다."),
    NOT_FOUND_FCM_TOKEN(HttpStatus.NOT_FOUND, "해당 사용자에게 등록된 FCM 토큰이 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
