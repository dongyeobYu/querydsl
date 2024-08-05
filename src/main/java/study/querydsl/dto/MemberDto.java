package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection        // -> QMemberDto 생성해줌, 타입 맞춰줌.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
