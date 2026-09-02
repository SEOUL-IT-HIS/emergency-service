package kr.co.seoulit.his.emergencyservice.disposition.service;

import kr.co.seoulit.his.emergencyservice.disposition.dto.*;

public interface DispositionService {
    DispositionDto createDisposition(DispositionCreateRequestDto request);
    AdmissionRequestDto createAdmissionRequest(String dispositionId, AdmissionRequestCreateDto request);
    TransferNoteDto createTransferNote(String dispositionId, TransferNoteCreateDto request);
    AmbulanceTransportDto createAmbulanceTransport(String dispositionId, AmbulanceTransportCreateDto request);
}
