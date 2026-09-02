package kr.co.seoulit.his.emergencyservice.code.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.code.dto.*;
import kr.co.seoulit.his.emergencyservice.code.entity.EmgCode;
import kr.co.seoulit.his.emergencyservice.code.entity.EmgCodeGroup;
import kr.co.seoulit.his.emergencyservice.code.mapper.EmgCodeMapstructMapper;
import kr.co.seoulit.his.emergencyservice.code.repository.EmgCodeGroupRepository;
import kr.co.seoulit.his.emergencyservice.code.repository.EmgCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmgCodeServiceImpl implements EmgCodeService {

    private final EmgCodeGroupRepository codeGroupRepository;
    private final EmgCodeRepository codeRepository;
    private final EmgCodeMapstructMapper codeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EmgCodeGroupDto> getCodes() {
        return codeGroupRepository.findAll().stream().map(group -> {
            EmgCodeGroupDto dto = codeMapper.toGroupDto(group);
            dto.setCodes(codeMapper.toCodeDtoList(
                    codeRepository.findByCodeGroup_GroupCodeOrderBySortOrderAsc(group.getGroupCode())));
            return dto;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmgCodeDto> getCodesByGroup(String groupCode) {
        return codeMapper.toCodeDtoList(
                codeRepository.findByCodeGroup_GroupCodeOrderBySortOrderAsc(groupCode));
    }

    @Override
    @Transactional
    public EmgCodeDto createCode(EmgCodeCreateRequestDto request) {
        if (!StringUtils.hasText(request.getGroupCode()) || !StringUtils.hasText(request.getCodeValue())) {
            throw new IllegalArgumentException("groupCode and codeValue are required");
        }
        EmgCodeGroup group = codeGroupRepository.findByGroupCode(request.getGroupCode())
                .orElseGet(() -> {
                    EmgCodeGroup g = new EmgCodeGroup();
                    g.setGroupCode(request.getGroupCode());
                    g.setGroupName(request.getGroupName());
                    g.setDescription(request.getGroupDescription());
                    g.setUseYn("Y");
                    g.setSortOrder(0);
                    g.setCreatedAt(LocalDateTime.now());
                    g.setUpdatedAt(LocalDateTime.now());
                    return codeGroupRepository.save(g);
                });

        EmgCode code = new EmgCode();
        code.setCodeGroup(group);
        code.setCodeValue(request.getCodeValue());
        code.setCodeName(request.getCodeName());
        code.setDescription(request.getDescription());
        code.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        code.setUseYn("Y");
        code.setCreatedAt(LocalDateTime.now());
        code.setUpdatedAt(LocalDateTime.now());
        return codeMapper.toCodeDto(codeRepository.save(code));
    }

    @Override
    @Transactional
    public EmgCodeDto updateCode(String codeId, EmgCodeUpdateRequestDto request) {
        EmgCode code = codeRepository.findById(codeId)
                .orElseThrow(() -> ResourceNotFoundException.of("code", codeId));
        if (request.getCodeName() != null) {
            code.setCodeName(request.getCodeName());
        }
        if (request.getDescription() != null) {
            code.setDescription(request.getDescription());
        }
        if (request.getSortOrder() != null) {
            code.setSortOrder(request.getSortOrder());
        }
        code.setUpdatedAt(LocalDateTime.now());
        return codeMapper.toCodeDto(code);
    }

    @Override
    @Transactional
    public EmgCodeDto updateUseYn(String codeId, EmgCodeUseYnRequestDto request) {
        if (!"Y".equals(request.getUseYn()) && !"N".equals(request.getUseYn())) {
            throw new IllegalArgumentException("useYn must be Y or N");
        }
        EmgCode code = codeRepository.findById(codeId)
                .orElseThrow(() -> ResourceNotFoundException.of("code", codeId));
        code.setUseYn(request.getUseYn());
        code.setUpdatedAt(LocalDateTime.now());
        return codeMapper.toCodeDto(code);
    }
}
