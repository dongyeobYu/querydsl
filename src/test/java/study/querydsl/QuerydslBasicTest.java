package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    /**
     *  member.age.in(10, 20)  -> age in (10, 20)
     *  member.age.between(10, 30) -> between 10, 30
     *
     *  member.age.gt(30)  -> age > 30
     *      *  member.age.goe(30) -> age >= 30
     *  member.age.loe(30) -> age <= 30
     *  member.age.lt(30) -> age < 30
     *
     *  member.username.like("member%") -> like
     *  member.username.contains("member") -> like %member%
     *  member.username.startWith("member") -> like member%
     * */


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

    /**
     * 회원 정렬
     * 1. 회원 나이 내림차순 desc
     * 2. 회원 이름 올림차순
     * 단, 회원 이름이 없으면 마지막에 출력(nulls last)
     * */
    @DisplayName("회원 정렬 체크")
    @Test
    public void sortTest () throws Exception{
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc())
                .orderBy(member.username.asc().nullsLast())
                .fetch();


        Assertions.assertThat(findMember.get(0).getUsername()).isEqualTo("member5");
        Assertions.assertThat(findMember.get(1).getUsername()).isEqualTo("member6");
        Assertions.assertThat(findMember.get(2).getUsername()).isEqualTo(null);
    }

    @DisplayName("페이징 처리")
    @Test
    public void paging1 () throws Exception{
        List<Member> findMember = queryFactory
                .select(member)
                .from(member)
                .orderBy(member.username.desc())
                .offset(0)                          // 몇번째 페이지
                .limit(10)                          // 몇개씩 끊어서
                .fetch();

        for (Member member1 : findMember) {
            System.out.println("member1.getUsername() = " + member1.getUsername());
            System.out.println("member1.getAge() = " + member1.getAge());
        }

        Assertions.assertThat(findMember.size()).isEqualTo(4);

    }

    @DisplayName("페이징 처리 + 카운트")
    @Test
    public void paging2 () throws Exception{
        List<Member> findMember = queryFactory
                .select(member)
                .from(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        Long l = queryFactory
                .select(member.count())
                .from(member)
                .fetchFirst();

        System.out.println("l = " + l);
    }
    
    @DisplayName("집합")
    @Test
    public void aggregation () throws Exception{
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.max(),
                        member.age.min(),
                        member.age.avg()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        System.out.println("tuple.get(member.age.sum()) = " + tuple.get(member.age.sum()));
        System.out.println("tuple.get(member.age.sum()) = " + tuple.get(member.age.max()));
        System.out.println("tuple.get(member.age.sum()) = " + tuple.get(member.age.min()));
        System.out.println("tuple.get(member.age.avg()) = " + tuple.get(member.age.avg()));

    }

    /**
     * 팀의 이름과 평균 연령을 구해라.
     * */
    @DisplayName("그룹")
    @Test
    public void group () throws Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 팀 A에 소속된 멤버의 이름을 찾아라.
     *  */
    @DisplayName("기본조인")
    @Test
    public void basicJoin() throws Exception{
        List<Member> fetch = queryFactory
                .select(member)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();


        Assertions.assertThat(fetch.size()).isEqualTo(2);
        Assertions.assertThat(fetch).extracting("username").containsExactly("member1", "member2");
    }

    /**
     * 회원이름과 팀이름이 같은 사람을 뽑아라 (연관관계 X)
     * -> 외부 조인 불가능 -> on 절을 사용하면 가능
     * */
    @DisplayName("세타 조인")
    @Test
    public void theta_join() throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 전부 조회
     * */
    @DisplayName("on 조인")
    @Test
    public void join_on_filtering() throws Exception{
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     *  회원과 팀 이름이 같은 사람을 외부조인해라.
     * */
    @DisplayName("연관관계가 없는 관계를 외부조인(left)")
    @Test
    public void join_on_no_relation() throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))   // leftJoin 안에 member.team, team 을 넣으면 ID로 매칭을 함.
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    @PersistenceUnit
    EntityManagerFactory emf;
    
    @DisplayName("페치조인 X")
    @Test
    public void fetchJoin_no() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //getPersistenceUnitUtil().isLoaded -> loading 이 된건지 안된건지.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        Assertions.assertThat(loaded).as("페치조인 미적용").isFalse();

    }

    @DisplayName("페치조인 O")
    @Test
    public void fetchJoin() throws Exception{
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        //getPersistenceUnitUtil().isLoaded -> loading 이 된건지 안된건지.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        Assertions.assertThat(loaded).as("페치조인 적용").isTrue();

    }

    @DisplayName("서브쿼리")
    @Test
    public void subQuery() throws Exception{

        QMember memberSub = new QMember("MemberSub");

        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result.get(0).getAge()).isEqualTo(40);
    }

    @DisplayName("서브쿼리, 나이가 평균 이상")
    @Test
    public void subQuery_goe() throws Exception{

        QMember memberSub = new QMember("MemberSub");

        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(2);
    }

    @DisplayName("서브쿼리, 나이가 가장 작은사람이랑 많은사람")
    @Test
    public void subQuery_in() throws Exception{

        QMember memberSub = new QMember("MemberSub");

        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.eq(
                                        JPAExpressions
                                                .select(memberSub.age.min())
                                                .from(memberSub)
                                        ).or(memberSub.age.eq(
                                                JPAExpressions
                                                        .select(memberSub.age.max())
                                                        .from(memberSub)
                                        )))
                ))
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(2);
    }

    @DisplayName("셀렉트절 서브쿼리")
    @Test
    public void selectSubQuery() throws Exception{

        QMember memberSub = new QMember("MemberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @DisplayName("Case문")
    @Test
    public void basicCase() throws Exception{
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    @DisplayName("복잡한 Case문")
    @Test
    public void complexCase() throws Exception{
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @DisplayName("상수")
    @Test
    public void constant() throws Exception{
        List<Tuple> fetch = queryFactory
                .select(member.username, Expressions.constant("A"))         // -> [member1, A] ....
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @DisplayName("문자 더하기")
    @Test
    public void concat() throws Exception{

        // ex) username_age
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }


    @DisplayName("Setter 를 이용한 DTO 조회")
    @Test
    public void findDtoBySetter() throws Exception{
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age)
                )
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("Field 를 이용한 DTO 조회")
    @Test
    public void findDtoByField() throws Exception{
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age)
                )
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 변수명이 안맞아도 타입만 맞으면 됌
     * */
    @DisplayName("생성자를 이용한 DTO 조회")
    @Test
    public void findDtoByConstructor() throws Exception{
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age)
                )
                .from(member)
                .fetch();

        for (UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /**
     * 변수명이 맞아야함
     * ExpressionUtils.as(subQuery, alias)
     * */
    @DisplayName("Field 를 이용한 DTO 조회 -> alias 를 줘야함 그래야 매핑됨 ")
    @Test
    public void findUserDto() throws Exception{

        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                        )
                )
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto.getName() + "_" + userDto.getAge());
        }
    }

    @DisplayName("쿼리프로젝션 사용")
    @Test
    public void findDtoQueryProjection(){
        // 단점 : QMemberDto 생성, MemberDto 는 QueryDsl의 의존성을 가짐
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("BooleanBuilder 사용")
    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, null);

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /**
     * BooleanBuilder 사용
     * */
    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder builder = new BooleanBuilder();
        if(usernameParam != null){
            builder.and(member.username.eq(usernameParam));
        }

        if(ageParam != null){
            builder.and(member.age.eq(ageParam));
        }

        return queryFactory
                .select(member)
                .from(member)
                .where(builder)
                .fetch();
    }

    @DisplayName("동적쿼리, where 다중 파라미터")
    @Test
    public void dynamicQuery_WhereParam() throws Exception{
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, null);


    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .select(member)
                .from(member)
                .where(usernameEq(usernameParam), ageEq(ageParam))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameParam) {
        return usernameParam != null ? member.username.eq(usernameParam) : null;
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam != null ? member.age.eq(ageParam) : null;
    }

    //광고 상태 isServiceable(서비스 가능), 날짜가 IN ,,,
}
