package kr.co.seoulit.his.emergencyservice.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisSessionConfig {

    /** "@class" 에 적힌 클래스를 되살릴 때 허용할 범위 (SessionUser 가 이 아래 있다) */
    private static final String ALLOWED_PACKAGE = "kr.co.seoulit.his.";

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        // 우리 패키지만 허용한다.
        // java.lang. 을 열어두면 ProcessBuilder 같은 위험한 클래스까지 들어온다.
        PolymorphicTypeValidator allowedTypes = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(ALLOWED_PACKAGE)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
                allowedTypes,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
