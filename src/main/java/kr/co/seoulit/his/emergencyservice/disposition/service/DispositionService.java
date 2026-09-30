package kr.co.seoulit.his.emergencyservice.disposition.service;

import java.util.List;

import kr.co.seoulit.his.emergencyservice.disposition.dto.*;

public interface DispositionService {
    DispositionDto createDisposition(DispositionCreateRequestDto request);
    List<DispositionDto> getDispositions(String receptionId);
    AdmissionRequestDto createAdmissionRequest(String dispositionId, AdmissionRequestCreateDto request);
    TransferNoteDto createTransferNote(String dispositionId, TransferNoteCreateDto request);
}
