
## 요구사항

### 요구사항 1: 동시성 제어 - "마지막 남은 VIP 좌석 1개를 100명이 동시에..."
시나리오: 블랙핑크 콘서트의 마지막 남은 VIP석 1개를 100명의 사용자가 정확히 동일한 밀리초에 예매 버튼을 클릭합니다. 시스템은 단 1명에게만 좌석을 할당하고, 나머지 99명에게는 "이미 선택된 좌석입니다"라는 메시지를 정확하게 보여줘야 합니다.

도전 과제:

ScheduleSeat 엔티티의 상태를 AVAILABLE에서 RESERVED로 변경하는 로직을 어떻게 동시성 문제 없이 처리할 것인가?

**낙관적 락(@Version)**을 적용하고, 버전 충돌 예외(ObjectOptimisticLockingFailureException)를 잡아서 사용자에게 적절한 피드백을 주는 로직을 구현해야 합니다.

더 나아가, "이 좌석만큼은 절대 중복되면 안 된다"는 강력한 요구사항 하에, 특정 트랜잭션 구간에서 **비관적 락(LockModeType.PESSIMISTIC_WRITE)**을 걸어 DB 레벨에서 다른 접근을 막는 방법을 구현해 보세요. 두 락킹 방식의 성능과 트레이드오프를 비교 분석해야 합니다.

### 요구사항 2: 성능 최적화 - "공연 상세 페이지, 1초 안에 모든 정보 로딩"
시나리오: 사용자가 특정 공연 상세 페이지에 진입하면, 아래 정보들이 단 한 번의 페이지 로딩으로 모두 표시되어야 합니다.

공연의 기본 정보 (제목, 포스터 등)

해당 공연의 모든 회차 목록 (날짜, 시간)

각 회차별, 좌석 등급별(VIP, R, S) 남은 좌석 수

각 좌석 등급별 가격 정보

도전 과제:

이 요구사항을 순진하게 구현하면 1(공연) + N(회차 수) + M(좌석 등급 수) 만큼의 쿼리가 발생하는 N+1 문제의 끝판왕에 직면하게 됩니다.

ToOne 관계는 JOIN FETCH로 해결하고, ToMany 관계인 회차 목록은 어떻게 가져올 것인가?

좌석 수와 가격 정보는 어떻게 효율적으로 집계할 것인가? JPQL의 서브쿼리나 스칼라 프로젝션을 사용해야 할 수 있습니다.

**@EntityGraph**를 사용하여 특정 조회에 필요한 연관관계 그래프를 동적으로 정의하고, 불필요한 조인을 피하는 전략을 구사해야 합니다.

최종적으로는 엔티티 그래프가 너무 복잡해져 성능이 나오지 않는 상황에 직면하고, **JPA DTO 프로젝션(생성자 표현식)**을 통해 처음부터 읽기 전용 DTO로 조회하는 것이 왜 더 나은 선택인지 깨닫게 될 것입니다.

### 요구사항 3: 복잡한 상태 관리 - "10분간의 좌석 임시 선점 및 자동 해제"
시나리오: 사용자가 좌석을 선택하면, 해당 좌석은 10분 동안 '임시 선점' 상태가 됩니다. 사용자가 10분 내에 결제를 완료하면 '결제 완료' 상태가 됩니다. 만약 10분이 지나도 결제하지 않으면, 해당 좌석은 다시 '예매 가능' 상태가 되어 다른 사용자가 선택할 수 있어야 합니다.

도전 과제:

'임시 선점' 상태가 된 좌석들을 어떻게 추적하고, 10분이 지난 좌석을 자동으로 '예매 가능' 상태로 되돌릴 것인가?

Spring의 @Scheduled 같은 스케줄러를 사용하여 주기적으로 만료된 좌석을 찾아 상태를 변경하는 배치 작업을 구현해야 합니다.

이때, 수십만 건의 만료된 좌석을 for 루프를 돌며 하나씩 save() 하는 것은 비효율의 극치입니다. **JPQL의 벌크 UPDATE 쿼리(@Modifying 어노테이션 사용)**를 통해 단 한 번의 쿼리로 모든 만료 좌석의 상태를 변경하는 방법을 구현해야 합니다.

벌크 연산 후 영속성 컨텍스트와의 데이터 불일치 문제가 발생할 수 있는데, 이를 어떻게 해결할 것인지(컨텍스트 초기화 등) 고민해야 합니다.

### 요구사항 4: 데이터 히스토리 관리 - "예매 취소 내역 추적 및 환불 정책 적용"
시나리오: 사용자가 예매를 취소합니다. 시스템은 단순히 데이터를 삭제하는 것이 아니라, 누가, 언제 취소했는지 기록을 남겨야 합니다. 또한, 취소 시점에 따라(공연 7일 전, 3일 전, 당일 등) 차등 환불 정책을 적용해야 합니다.

도전 과제:

Reservation 이나 ScheduleSeat의 상태를 CONFIRMED에서 CANCELLED로 변경할 때, 기존 데이터를 UPDATE하는 대신 어떻게 히스토리를 관리할 것인가?

**논리적 삭제(Soft Delete)**를 도입해야 합니다. @SQLDelete 어노테이션을 사용하여 DELETE 쿼리가 날아올 때 실제로는 status를 CANCELLED로 바꾸고, @Where 어노테이션으로 항상 status가 CANCELLED가 아닌 데이터만 조회하도록 엔티티 레벨에서 제어해야 합니다.

JPA Auditing(@CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy)을 적용하여 모든 데이터의 생성/수정 이력을 자동으로 기록해야 합니다. 이를 위해 @EntityListeners(AuditingEntityListener.class) 설정이 필요합니다.

취소 시점에 따라 환불 금액을 계산하는 복잡한 비즈니스 로직을 도메인 모델(Reservation 엔티티) 내에 캡슐화해야 합니다.

### 요구사항 5: 동적 쿼리 - "다양한 조건의 공연 검색 기능"
시나리오: 사용자가 공연을 검색할 때, 여러 필터를 조합하여 사용할 수 있어야 합니다. 예를 들어, "서울 지역에서, 다음 달에 열리는, '뮤지컬' 카테고리의, 제목에 '오페라'가 포함된 공연" 등을 검색할 수 있어야 합니다. 이 모든 필터는 선택사항입니다.

도전 과제:

검색 조건이 동적으로 변경되기 때문에, 고정된 JPQL 문자열만으로는 모든 경우의 수를 처리하기 어렵습니다. 수많은 if-else 분기문으로 JPQL 문자열을 조립하는 것은 유지보수 재앙을 초래합니다.

이 문제를 해결하기 위해 QueryDSL를 도입해야 합니다. 자바 코드로 쿼리를 작성하여 컴파일 시점에 쿼리의 문법적 오류를 잡고, 동적인 where 절을 안전하고 깔끔하게 구성하는 방법을 익혀야 합니다.

검색 결과 화면에서는 공연 정보와 함께 대표 회차의 시간, 기본 가격 등 연관된 정보를 함께 보여줘야 하므로, 동적 쿼리와 DTO 프로젝션을 결합하는 능력이 필요합니다.


### 요구사항 6: 객체지향적 모델링 - "유연한 할인 쿠폰 시스템 설계"
시나리오: 시스템에는 두 종류의 할인 쿠폰이 존재합니다. 하나는 정액 할인 쿠폰(예: 10,000원 할인), 다른 하나는 정률 할인 쿠폰(예: 10% 할인)입니다. 사용자는 예매 시 이 중 하나의 쿠폰을 적용할 수 있어야 하며, 앞으로 새로운 종류의 쿠폰(예: 특정 좌석 등급 전용 쿠폰)이 추가될 수 있습니다.

도전 과제:

각기 다른 계산 로직을 가진 쿠폰들을 어떻게 단일한 인터페이스로 처리할 것인가? 이는 객체지향의 다형성을 JPA에서 어떻게 구현하는지에 대한 도전입니다.

Coupon이라는 추상 클래스 또는 인터페이스를 두고, 이를 상속받는 FixedAmountCoupon, PercentageCoupon 엔티티를 만들어야 합니다.

JPA의 상속 관계 매핑 전략(@Inheritance)을 사용해야 합니다. 모든 자식 클래스를 한 테이블에 저장하는 SINGLE_TABLE 전략과, 각각 다른 테이블로 나누는 JOINED 전략의 장단점을 비교하고 이 상황에 맞는 전략을 선택해야 합니다.

예매 로직에서는 Coupon 타입의 참조만으로 실제 객체가 FixedAmountCoupon이든 PercentageCoupon이든 상관없이 할인 금액을 계산하는 다형적인 코드를 작성해야 합니다.

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

요구사항별 엔티티 설계 보강
## 요구사항 4: 데이터 히스토리 관리 (엔티티 수정)
이 요구사항은 새로운 엔티티보다는 기존 Reservation, ScheduleSeat 엔티티를 수정하는 것에 가깝습니다.

논리적 삭제(Soft Delete): status 필드를 CANCELLED로 변경하는 방식으로 처리하므로 별도 필드 추가는 필요 없을 수 있습니다.

JPA Auditing: 생성/수정 이력을 위해 별도의 추상 클래스를 만들어 상속받게 하는 것이 일반적입니다.

BaseEntity.java (추상 클래스 생성)

Java

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@MappedSuperclass // 이 클래스는 테이블과 매핑되지 않고, 자식 클래스에게 필드만 상속해줌
@EntityListeners(AuditingEntityListener.class) // Auditing 기능 활성화
public abstract class BaseEntity {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
Reservation.java, ScheduleSeat.java 등 주요 엔티티 수정

Java

// public class Reservation extends BaseEntity { ... }
// public class ScheduleSeat extends BaseEntity { ... }
이렇게 상속만 받으면 createdAt, updatedAt 필드가 자동으로 관리됩니다.

## 요구사항 5: 동적 쿼리 (엔티티 변경 없음)
이 요구사항은 기존 엔티티 모델을 변경하지 않습니다. QueryDSL이나 Criteria는 이미 존재하는 Performance, Schedule, Venue 등의 엔티티 관계를 기반으로 쿼리를 동적으로 생성하는 기술이므로, 엔티티 설계 자체에는 영향을 주지 않습니다.

## 요구사항 6: 유연한 할인 쿠폰 시스템 (신규 엔티티 설계 필수)
이 요구사항은 객체지향의 상속과 다형성을 데이터베이스로 모델링해야 하므로 새로운 엔티티 설계가 반드시 필요합니다.

Coupon (추상 클래스)
정액/정률 쿠폰의 공통 속성을 가집니다. JPA의 상속 관계 매핑을 사용합니다.

Java

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // 단일 테이블 전략 사용
@DiscriminatorColumn(name = "coupon_type") // DTYPE 컬럼 생성
public abstract class Coupon extends BaseEntity {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    private String name;
    private String code; // 쿠폰 코드
    private LocalDate expirationDate; // 만료일

    // 할인액을 계산하는 추상 메서드
    public abstract BigDecimal calculateDiscount(BigDecimal originalPrice);
}
FixedAmountCoupon (정액 할인 쿠폰)

Java

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("FIXED") // DTYPE 값
public class FixedAmountCoupon extends Coupon {

    private BigDecimal discountAmount; // 고정 할인 금액

    @Override
    public BigDecimal calculateDiscount(BigDecimal originalPrice) {
        return discountAmount;
    }
}
PercentageCoupon (정률 할인 쿠폰)

Java

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("PERCENTAGE") // DTYPE 값
public class PercentageCoupon extends Coupon {

    private int discountRate; // 할인율 (ex. 10%)

    @Override
    public BigDecimal calculateDiscount(BigDecimal originalPrice) {
        return originalPrice.multiply(BigDecimal.valueOf(discountRate / 100.0));
    }
}
이제 예매 시 어떤 쿠폰이 적용되었는지 기록하기 위해 Reservation 엔티티를 수정해야 합니다.

Reservation.java (쿠폰 관계 추가)

Java

@Entity
public class Reservation extends BaseEntity {
// ... 기존 필드

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon appliedCoupon; // 적용된 쿠폰
    
    private BigDecimal originalPrice; // 원래 가격
    private BigDecimal discountedPrice; // 할인 적용된 최종 가격
}
이렇게 하면 요구사항 6번을 구현하기 위한 엔티티 설계가 완성됩니다.

방금 설계한 쿠폰 엔티티들을 실제 예매 프로세스에 적용하는 서비스 로직을 구현해 보는 것은 어떠신가요? 사용자가 쿠폰 코드를 제시했을 때, 해당 쿠폰의 유효성을 검증하고, 종류에 따라 할인액을 계산하여 최종 결제 금액에 반영하는 ReservationService의 메서드를 함께 작성해 볼 수 있습니다.


1. User 엔티티의 email
   사용자(User)를 식별할 때, 시스템 내부에서는 id를 사용하지만 비즈니스 로직(로그인, 사용자 검색 등)에서는 이메일(email)을 훨씬 더 자주 사용합니다. 이메일은 유일하며, 사용자를 식별하는 완벽한 자연 식별자입니다.

Java

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
@NaturalIdCache // NaturalId 조회를 위한 캐시 활성화
public class User {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    private String name;

    @NaturalId // email을 자연 식별자로 지정
    @Column(unique = true, nullable = false)
    private String email;

    // ...
}
@NaturalId: email 필드가 이 엔티티의 자연 식별자임을 선언합니다.

@NaturalIdCache: 활성화하면 Hibernate는 email 값과 id(PK) 값을 매핑하는 별도의 캐시 영역을 관리합니다. 덕분에 이메일로 사용자를 찾을 때 DB를 조회하지 않고도 PK를 바로 알아낼 수 있어 성능이 향상됩니다.

2. Coupon 엔티티의 code
   쿠폰 역시 id보다는 **쿠폰 코드(code)**로 조회하는 경우가 압도적으로 많습니다. "SUMMER2025"와 같은 쿠폰 코드는 유일하며 비즈니스적으로 핵심적인 값입니다.

Java

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "coupon_type")
@NaturalIdCache
public abstract class Coupon extends BaseEntity {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    private String name;

    @NaturalId // code를 자연 식별자로 지정
    @Column(unique = true, nullable = false)
    private String code; // 쿠폰 코드

    // ...
}
이렇게 설정하면, 사용자가 입력한 쿠폰 코드로 쿠폰 정보를 조회하는 로직의 성능을 크게 향상시킬 수 있습니다.