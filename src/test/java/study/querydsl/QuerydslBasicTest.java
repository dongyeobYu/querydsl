package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        // 필드로 빼기 가능
        queryFactory = new JPAQueryFactory(em);
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

    }

    @DisplayName("JPQL")
    @Test
    public void startJPQL () throws Exception{
        //Member1을 찾아라.
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("querydsl 시작")
    @Test
    public void startQuerydsl() throws Exception{

        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @DisplayName("쿼리 dsl 스태틱 임포트")
    @Test
    public void middleQuerydsl() throws Exception{

        // QMember.Member -> Static Import
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @DisplayName("Search")
    @Test
    public void search () throws Exception{
        /*Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10))
                        .and(member.age.between(10, 30))
                )
                .fetchOne();*/


        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10),
                        member.age.between(10, 30)
                )
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

        /**
         *  member.age.in(10, 20)  -> age in (10, 20)
         *  member.age.between(10, 30) -> between 10, 30
         *
         *  member.age.goe(30) -> age >= 30
         *  member.age.gt(30)  -> age > 30
         *  member.age.loe(30) -> age <= 30
         *  member.age.lt(30) -> age < 30
         *
         *  member.username.like("member%") -> like
         *  member.username.contains("member") -> like %member%
         *  member.username.startWith("member") -> like member%
         * */

    }

    @DisplayName("result Fetch")
    @Test
    public void resultFetch () throws Exception{
        // List
        List<Member> fetch = queryFactory
                .select(member)
                .from(member)
                .fetch();

        // Count
        Long count = queryFactory
                .select(member.count())
                .from(member)
                .fetchOne();

        // 단건 없으면 null, 결과가 둘 이상이면 NonUniqueResultException 발생
        Member member1 = queryFactory
                .select(member)
                .from(member)
                .fetchOne();

        // List 조회 후 limit(1)
        Member member2 = queryFactory
                .select(member)
                .from(member)
                .fetchFirst();

    }


}
