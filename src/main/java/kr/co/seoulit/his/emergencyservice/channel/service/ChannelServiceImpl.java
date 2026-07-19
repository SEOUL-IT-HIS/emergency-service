package kr.co.seoulit.his.emergencyservice.channel.service;

import kr.co.seoulit.his.emergencyservice.channel.dto.*;
import kr.co.seoulit.his.emergencyservice.channel.entity.ConsultRequest;
import kr.co.seoulit.his.emergencyservice.channel.entity.OncallRequest;
import kr.co.seoulit.his.emergencyservice.channel.repository.ConsultRequestRepository;
import kr.co.seoulit.his.emergencyservice.channel.repository.OncallRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ConsultRequestRepository consultRequestRepository;
    private final OncallRequestRepository oncallRequestRepository;

    @Override
    @Transactional
    public ConsultRequestDto createConsultation(ConsultCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getSpecialty())) {
            throw new IllegalArgumentException("encounterId and specialty are required");
        }
        ConsultRequest entity = new ConsultRequest();
        entity.setReceptionNo(request.getEncounterId());
        entity.setTargetDeptCode(request.getSpecialty());
        entity.setReason(request.getReason());
        entity.setOrderId(request.getOrderId());
        entity.setConsultStatusCode("REQUESTED");
        entity.setRequestedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        ConsultRequest saved = consultRequestRepository.save(entity);

        ConsultRequestDto dto = new ConsultRequestDto();
        dto.setId(saved.getId());
        dto.setReceptionNo(saved.getReceptionNo());
        dto.setTargetDeptCode(saved.getTargetDeptCode());
        dto.setConsultStatusCode(saved.getConsultStatusCode());
        dto.setReason(saved.getReason());
        dto.setOrderId(saved.getOrderId());
        dto.setRequestedAt(saved.getRequestedAt());
        return dto;
    }

    @Override
    @Transactional
    public OncallRequestDto createOnCallPage(OncallCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getTargetRole())) {
            throw new IllegalArgumentException("encounterId and targetRole are required");
        }
        OncallRequest entity = new OncallRequest();
        entity.setReceptionNo(request.getEncounterId());
        entity.setTargetRoleCode(request.getTargetRole());
        entity.setCalledById(request.getCalledById());
        entity.setCalledAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        OncallRequest saved = oncallRequestRepository.save(entity);

        OncallRequestDto dto = new OncallRequestDto();
        dto.setId(saved.getId());
        dto.setReceptionNo(saved.getReceptionNo());
        dto.setTargetRoleCode(saved.getTargetRoleCode());
        dto.setCalledById(saved.getCalledById());
        dto.setCalledAt(saved.getCalledAt());
        return dto;
    }
}
