package kr.co.seoulit.his.emergencyservice.disposition.service;

import java.util.List;

import kr.co.seoulit.his.emergencyservice.disposition.dto.*;

public interface DispositionService {
    DispositionDto createDisposition(DispositionCreateRequestDto request);
    List<DispositionDto> getDispositions(String receptionId);
    AdmissionRequestDto createAdmissionRequest(String dispositionId, AdmissionRequestCreateDto request);
    List<AdmissionRequestDto> getAdmissionRequests(String dispositionId);
    /** 병동 회신(Kafka)으로 입원요청 상태를 바꾼다. REST 로 열지 않는다(개발표준 21.3). */
    AdmissionRequestDto updateAdmissionStatus(String dispositionId, String admissionRequestId, String statusCode, String wardCode);
    TransferNoteDto createTransferNote(String dispositionId, TransferNoteCreateDto request);
    List<TransferNoteDto> getTransferNotes(String dispositionId);
}
