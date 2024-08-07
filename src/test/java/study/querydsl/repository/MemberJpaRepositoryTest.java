package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

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

}