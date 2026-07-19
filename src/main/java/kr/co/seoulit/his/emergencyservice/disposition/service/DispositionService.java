package kr.co.seoulit.his.emergencyservice.disposition.service;

import kr.co.seoulit.his.emergencyservice.disposition.dto.*;

public interface DispositionService {
    DispositionDto createDisposition(DispositionCreateRequestDto request);
    AdmissionRequestDto createAdmissionRequest(Long dispositionId, AdmissionRequestCreateDto request);
    TransferNoteDto createTransferNote(Long dispositionId, TransferNoteCreateDto request);
    AmbulanceTransportDto createAmbulanceTransport(Long dispositionId, AmbulanceTransportCreateDto request);
}
