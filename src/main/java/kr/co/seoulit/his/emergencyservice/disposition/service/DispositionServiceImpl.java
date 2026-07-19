package kr.co.seoulit.his.emergencyservice.disposition.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.disposition.dto.*;
import kr.co.seoulit.his.emergencyservice.disposition.entity.*;
import kr.co.seoulit.his.emergencyservice.disposition.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DispositionServiceImpl implements DispositionService {

    private final DispositionRepository dispositionRepository;
    private final AdmissionRequestRepository admissionRequestRepository;
    private final TransferNoteRepository transferNoteRepository;
    private final AmbulanceTransportRepository ambulanceTransportRepository;

    @Override
    @Transactional
    public DispositionDto createDisposition(DispositionCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getDispositionType())) {
            throw new IllegalArgumentException("encounterId and dispositionType are required");
        }
        Disposition entity = new Disposition();
        entity.setReceptionNo(request.getEncounterId());
        entity.setDispositionTypeCode(request.getDispositionType());
        entity.setDecidedById(request.getDecidedById());
        entity.setDecidedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return toDispositionDto(dispositionRepository.save(entity));
    }

    @Override
    @Transactional
    public AdmissionRequestDto createAdmissionRequest(Long dispositionId, AdmissionRequestCreateDto request) {
        Disposition disposition = findDisposition(dispositionId);
        AdmissionRequest entity = new AdmissionRequest();
        entity.setDisposition(disposition);
        entity.setTargetDeptCode(StringUtils.hasText(request.getTargetDeptCode())
                ? request.getTargetDeptCode() : request.getWardPrefer());
        entity.setRequestStatusCode("REQUESTED");
        entity.setRequestedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        AdmissionRequest saved = admissionRequestRepository.save(entity);

        AdmissionRequestDto dto = new AdmissionRequestDto();
        dto.setId(saved.getId());
        dto.setDispositionId(disposition.getId());
        dto.setTargetDeptCode(saved.getTargetDeptCode());
        dto.setRequestStatusCode(saved.getRequestStatusCode());
        dto.setRequestedAt(saved.getRequestedAt());
        return dto;
    }

    @Override
    @Transactional
    public TransferNoteDto createTransferNote(Long dispositionId, TransferNoteCreateDto request) {
        Disposition disposition = findDisposition(dispositionId);
        TransferNote entity = new TransferNote();
        entity.setDisposition(disposition);
        entity.setTargetHospitalCode(request.getTargetHospitalCode());
        entity.setContent(request.getContent());
        entity.setWrittenById(request.getWrittenById());
        entity.setWrittenAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        TransferNote saved = transferNoteRepository.save(entity);

        TransferNoteDto dto = new TransferNoteDto();
        dto.setId(saved.getId());
        dto.setDispositionId(disposition.getId());
        dto.setTargetHospitalCode(saved.getTargetHospitalCode());
        dto.setContent(saved.getContent());
        dto.setWrittenById(saved.getWrittenById());
        dto.setWrittenAt(saved.getWrittenAt());
        return dto;
    }

    @Override
    @Transactional
    public AmbulanceTransportDto createAmbulanceTransport(Long dispositionId, AmbulanceTransportCreateDto request) {
        Disposition disposition = findDisposition(dispositionId);
        AmbulanceTransport entity = new AmbulanceTransport();
        entity.setDisposition(disposition);
        entity.setAmbulanceNo(request.getAmbulanceNo());
        entity.setTransportTypeCode(request.getTransportTypeCode());
        entity.setDepartedAt(request.getDepartedAt());
        entity.setArrivedAt(request.getArrivedAt());
        entity.setAccompanyingStaffId(request.getAccompanyingStaffId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        AmbulanceTransport saved = ambulanceTransportRepository.save(entity);

        AmbulanceTransportDto dto = new AmbulanceTransportDto();
        dto.setId(saved.getId());
        dto.setDispositionId(disposition.getId());
        dto.setAmbulanceNo(saved.getAmbulanceNo());
        dto.setTransportTypeCode(saved.getTransportTypeCode());
        dto.setDepartedAt(saved.getDepartedAt());
        dto.setArrivedAt(saved.getArrivedAt());
        dto.setAccompanyingStaffId(saved.getAccompanyingStaffId());
        return dto;
    }

    private Disposition findDisposition(Long id) {
        return dispositionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("disposition", id));
    }

    private DispositionDto toDispositionDto(Disposition entity) {
        DispositionDto dto = new DispositionDto();
        dto.setId(entity.getId());
        dto.setReceptionNo(entity.getReceptionNo());
        dto.setDispositionTypeCode(entity.getDispositionTypeCode());
        dto.setDecidedById(entity.getDecidedById());
        dto.setDecidedAt(entity.getDecidedAt());
        return dto;
    }
}
