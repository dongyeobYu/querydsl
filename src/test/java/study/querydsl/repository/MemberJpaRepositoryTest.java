package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Autowired
    JPAQueryFactory queryFactory;

    @DisplayName("findAll")
    @Test
    public void findAll() throws Exception{

        Member member = new Member("member1", 10);
        Member member2 = new Member("member2", 20);
        em.persist(member);
        em.persist(member2);

        em.flush();
        em.clear();

        List<Member> result = memberJpaRepository.findAll();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }

    }

    @DisplayName("search Test")
    @Test
    public void SearchTest() throws Exception{
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        // 동적쿼리는 limit 이나 paging 이 들어가야함
        MemberSearchCondition condition = new MemberSearchCondition();
        //condition.setAgeGoe(35);
        //condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberJpaRepository.search(condition);
        System.out.println("result = " + result);
    }

}