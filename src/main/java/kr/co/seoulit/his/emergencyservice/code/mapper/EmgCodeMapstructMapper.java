package kr.co.seoulit.his.emergencyservice.code.mapper;

import kr.co.seoulit.his.emergencyservice.code.dto.EmgCodeDto;
import kr.co.seoulit.his.emergencyservice.code.dto.EmgCodeGroupDto;
import kr.co.seoulit.his.emergencyservice.code.entity.EmgCode;
import kr.co.seoulit.his.emergencyservice.code.entity.EmgCodeGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface EmgCodeMapstructMapper {

    @Mapping(target = "codes", ignore = true)
    EmgCodeGroupDto toGroupDto(EmgCodeGroup group);

    @Mapping(target = "groupCode", source = "codeGroup.groupCode")
    EmgCodeDto toCodeDto(EmgCode code);

    List<EmgCodeDto> toCodeDtoList(List<EmgCode> codes);
}
