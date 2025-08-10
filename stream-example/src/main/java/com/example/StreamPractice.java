package com.example;
/*
 * Java Stream 연습 세트 — 데이터 + 30문제
 * ------------------------------------------------------------
 * - JDK 17 기준, DB 없이 모두 메모리에서 동작하도록 데이터를 생성합니다.
 * - 아래 DataFactory가 사람/상품/주문/리뷰/센서 이벤트를 랜덤 시드(고정)로 만들어 줍니다.
 * - 각 문제는 메서드 시그니처와 함께 TODO로 제공됩니다. (정답 구현은 비워둠)
 * - 실행은 `main`을 참고하세요. (기본값은 예제 실행 OFF)
 */

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StreamPractice {
    // ===== 데이터 모델 =====
    enum Gender {MALE, FEMALE}

    enum OrderStatus {NEW, PAID, SHIPPED, DELIVERED, CANCELED}

    record Person(String id, String name, int age, String city, Gender gender) {
    }

    record Product(String id, String category, String name, double price, double rating) {
    }

    record OrderLine(Product product, int qty) {
    }

    record Order(String id, Person customer, List<OrderLine> lines, LocalDateTime orderedAt, OrderStatus status) {
    }

    record Review(String productId, int stars, String comment) {
    }

    record SensorEvent(String deviceId, String type, double value, Instant timestamp) {
    }

    record Data(List<Person> persons,
                List<Product> products,
                List<Order> orders,
                List<Review> reviews,
                List<SensorEvent> sensorEvents) {
    }

    // ===== 랜덤 고정 시드 데이터 팩토리 =====
    static class DataFactory {
        private final Random rnd;

        DataFactory(long seed) {
            this.rnd = new Random(seed);
        }

        Data create() {
            var persons = persons(120);
            var products = products(160);
            var reviews = reviews(products, 0, 6); // 각 상품당 0~5개 리뷰
            var orders = orders(persons, products, 900); // 최근 120일
            var events = sensorEvents(1200);
            return new Data(persons, products, orders, reviews, events);
        }

        List<Person> persons(int n) {
            String[] first = {"Kim", "Lee", "Park", "Choi", "Jung", "Kang", "Cho", "Yoon", "Jang", "Im"};
            String[] second = {"Min", "Jae", "Seo", "Hye", "Young", "Soo", "Eun", "Hyun", "Jin", "Ah"};
            String[] cities = {"Seoul", "Busan", "Incheon", "Daegu", "Daejeon", "Gwangju", "Ulsan", "Suwon"};
            return IntStream.range(0, n).mapToObj(i -> {
                String name = first[rnd.nextInt(first.length)] + " " + second[rnd.nextInt(second.length)];
                return new Person(
                        "P%04d".formatted(i),
                        name,
                        18 + rnd.nextInt(48), // 18~65
                        cities[rnd.nextInt(cities.length)],
                        rnd.nextBoolean() ? Gender.MALE : Gender.FEMALE
                );
            }).toList();
        }

        List<Product> products(int n) {
            String[] cats = {"Electronics", "Books", "Grocery", "Fashion", "Sports", "Beauty", "Home"};
            return IntStream.range(0, n).mapToObj(i -> new Product(
                    "PR%04d".formatted(i),
                    cats[rnd.nextInt(cats.length)],
                    "Item-" + i,
                    // 5.0 ~ 2000.0 사이 분포 (전자 쪽이 좀 비싸게)
                    Math.round((5 + rnd.nextDouble() * (rnd.nextBoolean() ? 800 : 2000)) * 100.0) / 100.0,
                    // 1.0 ~ 5.0
                    Math.round((1 + rnd.nextDouble() * 4) * 10.0) / 10.0
            )).toList();
        }

        List<Review> reviews(List<Product> products, int min, int maxExclusive) {
            List<Review> out = new ArrayList<>();
            for (var p : products) {
                int k = rnd.nextInt(min, maxExclusive); // 0~5
                for (int i = 0; i < k; i++) {
                    int stars = 1 + rnd.nextInt(5);
                    out.add(new Review(p.id(), stars, "review-" + p.id() + "-" + i));
                }
            }
            return out;
        }

        List<Order> orders(List<Person> people, List<Product> products, int n) {
            List<Order> out = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < n; i++) {
                Person customer = people.get(rnd.nextInt(people.size()));
                int linesCnt = 1 + rnd.nextInt(5);
                List<OrderLine> lines = new ArrayList<>();
                for (int j = 0; j < linesCnt; j++) {
                    Product p = products.get(rnd.nextInt(products.size()));
                    int qty = 1 + rnd.nextInt(4);
                    lines.add(new OrderLine(p, qty));
                }
                LocalDateTime ts = now.minusDays(rnd.nextInt(120)).minusMinutes(rnd.nextInt(60 * 24));
                OrderStatus st = OrderStatus.values()[rnd.nextInt(OrderStatus.values().length)];
                out.add(new Order("O%05d".formatted(i), customer, List.copyOf(lines), ts, st));
            }
            return out;
        }

        List<SensorEvent> sensorEvents(int n) {
            String[] devices = {"dev-A", "dev-B", "dev-C"};
            String[] types = {"temp", "humidity"};
            Instant now = Instant.now();
            List<SensorEvent> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                String d = devices[rnd.nextInt(devices.length)];
                String t = types[rnd.nextInt(types.length)];
                double value = t.equals("temp")
                        ? 15 + rnd.nextDouble() * 15 // 15~30
                        : 30 + rnd.nextDouble() * 70; // 30~100
                Instant ts = now.minusSeconds(rnd.nextInt(86_400)); // 지난 하루
                out.add(new SensorEvent(d, t, Math.round(value * 10.0) / 10.0, ts));
            }
            return out;
        }
    }

    // ====== 연습 문제 (메서드 시그니처만 제공 — TODO 구현) ======
    // 힌트: 필요시 Collectors.groupingBy, mapping, flatMap, filtering, counting, summing*, averaging*, summarizing*, maxBy/minBy,
    //       collectingAndThen, partitioningBy, teeing(Java 12+), toMap, toSet, joining, Comparator.comparing* 등 사용

    /**
     * 1) 카테고리별로 가격 상위 N개 상품의 이름을 가격 내림차순으로 반환
     */
    static Map<String, List<String>> topNProductsByCategory(List<Product> products, int n) {
        throw new UnsupportedOperationException("TODO #1");
    }

    /**
     * 2) 도시별 평균 주문 금액 (Order.lines 의 (price*qty) 합)
     */
    static Map<String, Double> avgOrderAmountByCity(List<Order> orders) {
        throw new UnsupportedOperationException("TODO #2");
    }

    /**
     * 3) 가장 많이 팔린 상품 TOP K (수량 기준)
     */
    static List<Product> topKSellingProducts(List<Order> orders, int k) {
        throw new UnsupportedOperationException("TODO #3");
    }

    /**
     * 4) 고객별 총 지출액 상위 5명 (동률이면 이름순)
     */
    static List<Person> topSpenders(List<Order> orders) {
        throw new UnsupportedOperationException("TODO #4");
    }

    /**
     * 5) 주문이 한 번도 없는 상품 리스트 (안티 조인)
     */
    static List<Product> productsNeverOrdered(List<Product> products, List<Order> orders) {
        throw new UnsupportedOperationException("TODO #5");
    }

    /**
     * 6) 제품별 리뷰 평균 별점(Map<productId, avgStars>) — 리뷰가 없으면 미포함
     */
    static Map<String, Double> avgStarsByProduct(List<Review> reviews) {
        throw new UnsupportedOperationException("TODO #6");
    }

    /**
     * 7) 센서 타입별( temp/humidity ) 최근 30분 평균값
     */
    static Map<String, Double> avgSensorLast30Min(List<SensorEvent> events) {
        throw new UnsupportedOperationException("TODO #7");
    }

    /**
     * 8) 각 카테고리에서 평점이 가장 높은 상품(동률이면 가격 낮은 순)
     */
    static Map<String, Product> bestRatedByCategory(List<Product> products) {
        throw new UnsupportedOperationException("TODO #8");
    }

    /**
     * 9) 나이 기준 파티셔닝: 성인(>=20)과 미성년자(<20) 인원수
     */
    static Map<Boolean, Long> partitionAdultsCount(List<Person> persons) {
        throw new UnsupportedOperationException("TODO #9");
    }

    /**
     * 10) 주문 상태별 총 매출액 (CANCELED 제외)
     */
    static Map<OrderStatus, Double> revenueByStatus(List<Order> orders) {
        throw new UnsupportedOperationException("TODO #10");
    }

    /**
     * 11) 최근 7일간 일자별 주문 수(Map<LocalDate, count>)
     */
    static Map<LocalDate, Long> ordersCountByDayLast7(List<Order> orders) {
        throw new UnsupportedOperationException("TODO #11");
    }

    /**
     * 12) 제품명 대문자 조인 (특정 카테고리) — 예: "A, B, C"
     */
    static String joinUpperNamesByCategory(List<Product> products, String category) {
        throw new UnsupportedOperationException("TODO #12");
    }

    /**
     * 13) 주문 총액의 상위 10% cutoff 값(근사 퍼센타일)
     */
    static double p90OfOrderAmounts(List<Order> orders) {
        throw new UnsupportedOperationException("TODO #13");
    }

    /**
     * 14) 카테고리별 가격 SummaryStatistics(DoubleSummaryStatistics)
     */
    static Map<String, DoubleSummaryStatistics> priceStatsByCategory(List<Product> products) {
        throw new UnsupportedOperationException("TODO #14");
    }

    /**
     * 15) 이름 대소문자 무시하고 특정 이름의 첫 번째 고객 찾기 (Optional)
     */
    static Optional<Person> findByNameIgnoreCase(List<Person> persons, String name) {
        throw new UnsupportedOperationException("TODO #15");
    }

    // ===== 고급 문제 (16~30) =====
    // 난이도 상 — 조합/집계/슬라이딩 윈도/커스텀 Collector/중첩 grouping 등 스트림 심화 주제

    /**
     * 16) 고객별로 카테고리 지출 상위 K의 카테고리 이름을 지출 내림차순으로 반환
     * 예: {Person(P0001)->[Electronics, Books], ...}
     */
    static Map<Person, List<String>> topKCategoriesBySpendPerCustomer(List<Order> orders, int k) {
        throw new UnsupportedOperationException("TODO #16");
    }

    /**
     * 17) 고객별 주문 금액의 표준편차가 minStdDev 이상인 고객 집합
     * (힌트: 고객->금액 리스트를 만든 뒤 평균/분산 계산, 또는 커스텀 Collector)
     */
    static Set<Person> customersWithStddevAbove(List<Order> orders, double minStdDev) {
        throw new UnsupportedOperationException("TODO #17");
    }

    /**
     * 18) 상품별 주문 타임라인(시간 오름차순). 값은 주문이 발생한 시각 리스트
     * 예: {PR0001 -> [2025-07-10T12:03, 2025-07-12T09:10, ...], ...}
     */
    static Map<String, List<LocalDateTime>> productOrderTimeline(List<Order> orders) {
        throw new UnsupportedOperationException("TODO #18");
    }

    /**
     * 19) 함께 구매된 상품 쌍(co-purchase) TOP K (동일 주문 내의 서로 다른 상품 2개 조합).
     * (힌트: 각 주문의 라인업에서 조합(pair) 생성 -> pair별 카운트 -> 상위 K 정렬)
     */
    static List<ProductPair> topCoPurchasedPairs(List<Order> orders, int k) {
        throw new UnsupportedOperationException("TODO #19");
    }

    /**
     * 20) 디바이스별 이동 평균(moving average): window 길이와 step 간격으로 시간 버킷을 굴리며 평균 계산
     * 반환: device -> (버킷 시작시각 -> 평균값). (힌트: 그룹화 후 정렬, 슬라이딩 윈도)
     */
    static Map<String, NavigableMap<Instant, Double>> movingAvgPerDevice(List<SensorEvent> events, Duration window, Duration step) {
        throw new UnsupportedOperationException("TODO #20");
    }

    /**
     * 21) 센서 세션화(sessionization): 같은 디바이스에서 이벤트 간 간격이 inactivityGap을 넘으면 새 세션 시작
     * 반환: device -> 세션 목록(각 세션은 이벤트 리스트, 시각 오름차순)
     */
    static Map<String, List<List<SensorEvent>>> sessionizeByDevice(List<SensorEvent> events, Duration inactivityGap) {
        throw new UnsupportedOperationException("TODO #21");
    }

    /**
     * 22) 고객별 주문금액의 90퍼센타일(p90)을 구하고, p90 상위 N명의 고객(동률은 이름 오름차순)
     */
    static List<Person> topNCustomersByP90(List<Order> orders, int n) {
        throw new UnsupportedOperationException("TODO #22");
    }

    /**
     * 23) 도시-성별 2단계 그룹핑으로 나이 통계(IntSummaryStatistics) 계산
     * 반환: city -> (gender -> stats)
     */
    static Map<String, Map<Gender, IntSummaryStatistics>> ageStatsByCityAndGender(List<Person> persons) {
        throw new UnsupportedOperationException("TODO #23");
    }

    /**
     * 24) 카테고리별 "복합 점수"가 가장 높은 상품 (점수 = rating * w1 + log(1+reviewCount) * w2)
     * reviews를 사용해 reviewCount를 결합(join). 동률이면 가격이 낮은 상품 우선.
     */
    static Map<String, Product> bestProductPerCategoryByComposite(List<Product> products, List<Review> reviews, double w1, double w2) {
        throw new UnsupportedOperationException("TODO #24");
    }

    /**
     * 25) 도시별 최근 주문 K개 (주문시각 내림차순). 값은 Order 리스트
     * (힌트: groupingBy(city, collectingAndThen(정렬 후 limit)))
     */
    static Map<String, List<Order>> topKRecentOrdersPerCity(List<Order> orders, int k) {
        throw new UnsupportedOperationException("TODO #25");
    }

    /**
     * 26) 카테고리별 가격 히스토그램(binSize 단위). 반환: category -> ("[start,end)" -> 개수)
     * (힌트: floor(price/binSize)*binSize 로 버킷 시작을 구해 문자열 라벨 구성)
     */
    static Map<String, Map<String, Long>> priceHistogramByCategory(List<Product> products, double binSize) {
        throw new UnsupportedOperationException("TODO #26");
    }

    /**
     * 27) 전체 주문금액의 지니계수(Gini coefficient) 계산 (0=완전평등, 1=완전불평등)
     * (힌트: 금액 오름차순 정렬 -> 로렌츠 곡선 면적을 스트림 누적합으로 계산)
     */
    static double giniOfOrderAmounts(List<Order> orders) {
        throw new UnsupportedOperationException("TODO #27");
    }

    /**
     * 28) 고객별 카테고리 지출 비중(share-of-wallet): 고객 -> (카테고리 -> 지출/총지출)
     * (힌트: 두 번의 groupingBy 또는 teeing으로 총합과 카테고리 합을 함께 구해 normalize)
     */
    static Map<Person, Map<String, Double>> categoryShareOfWallet(List<Order> orders) {
        throw new UnsupportedOperationException("TODO #28");
    }

    /**
     * 29) 카테고리별 K번째로 저렴한 상품 (없으면 Optional.empty)
     * 반환: category -> Optional<Product>
     */
    static Map<String, Optional<Product>> kthCheapestPerCategory(List<Product> products, int k) {
        throw new UnsupportedOperationException("TODO #29");
    }

    /**
     * 30) 이름(대소문자 무시)별 중복 고객 제거 — 가장 나이가 어린(Person.age 최소)만 남김, 결과를 이름 오름차순으로 반환
     * (힌트: toMap의 merge 또는 groupingBy + minBy, collectingAndThen)
     */
    static List<Person> dedupeByNameKeepYoungest(List<Person> persons) {
        throw new UnsupportedOperationException("TODO #30");
    }

    // 보조 레코드: 상품 쌍 (사전식 정렬로 (a,b)와 (b,a)를 동일시)
    record ProductPair(String a, String b) {
        static ProductPair of(String x, String y) {
            return (x.compareTo(y) <= 0) ? new ProductPair(x, y) : new ProductPair(y, x);
        }
    }

    // ===== 유틸 =====
    static double orderAmount(Order o) {
        return o.lines().stream()
                .mapToDouble(ol -> ol.product().price() * ol.qty())
                .sum();
    }

    // ===== 실행 가이드 =====
    private static final boolean RUN_SAMPLE_PRINTS = false; // true로 바꾸면 데이터 미리보기 출력

    public static void main(String[] args) {
        var data = new DataFactory(42).create();
        System.out.printf("persons=%d, products=%d, orders=%d, reviews=%d, events=%d%n",
                data.persons().size(), data.products().size(), data.orders().size(), data.reviews().size(), data.sensorEvents().size());

        if (RUN_SAMPLE_PRINTS) {
            System.out.println("— 샘플 사람 3명 —");
            data.persons().stream().limit(3).forEach(System.out::println);
            System.out.println("— 샘플 상품 3개 —");
            data.products().stream().limit(3).forEach(System.out::println);
            System.out.println("— 샘플 주문 2개 (총액) —");
            data.orders().stream().limit(2).forEach(o -> System.out.println(o.id() + " => " + orderAmount(o)));
            System.out.println("— 샘플 리뷰 5개 —");
            data.reviews().stream().limit(5).forEach(System.out::println);
            System.out.println("— 샘플 센서 5개 —");
            data.sensorEvents().stream().limit(5).forEach(System.out::println);
        }

        // === 문제 정답 호출 샘플 ===
        var m1 = topNProductsByCategory(data.products(), 3);
        System.out.println("1) topNProductsByCategory[Electronics] -> " + m1.getOrDefault("Electronics", List.of()));

        var m2 = avgOrderAmountByCity(data.orders());
        System.out.println("2) avgOrderAmountByCity -> " + m2);

        var l3 = topKSellingProducts(data.orders(), 5);
        System.out.println("3) topKSellingProducts(5) -> " + l3.stream().map(Product::name).toList());

        var l4 = topSpenders(data.orders());
        System.out.println("4) topSpenders -> " + l4);

        var l5 = productsNeverOrdered(data.products(), data.orders());
        System.out.println("5) productsNeverOrdered count -> " + l5.size());

        var m6 = avgStarsByProduct(data.reviews());
        System.out.println("6) avgStarsByProduct entries -> " + m6.size());

        var m7 = avgSensorLast30Min(data.sensorEvents());
        System.out.println("7) avgSensorLast30Min -> " + m7);

        var m8 = bestRatedByCategory(data.products());
        System.out.println("8) bestRatedByCategory -> " + m8.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name())));

        var m9 = partitionAdultsCount(data.persons());
        System.out.println("9) partitionAdultsCount -> " + m9);

        var m10 = revenueByStatus(data.orders());
        System.out.println("10) revenueByStatus -> " + m10);

        var m11 = ordersCountByDayLast7(data.orders());
        System.out.println("11) ordersCountByDayLast7 -> " + new TreeMap<>(m11));

        var s12 = joinUpperNamesByCategory(data.products(), "Electronics");
        System.out.println("12) joinUpperNamesByCategory(Electronics) -> " +
                (s12.length() > 60 ? s12.substring(0, 60) + "..." : s12));

        double d13 = p90OfOrderAmounts(data.orders());
        System.out.println("13) p90OfOrderAmounts -> " + d13);

        var m14 = priceStatsByCategory(data.products());
        System.out.println("14) priceStatsByCategory[min,max,avg] -> " + m14.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> Map.of("min", e.getValue().getMin(), "max", e.getValue().getMax(), "avg", e.getValue().getAverage()))));

        var o15 = findByNameIgnoreCase(data.persons(), data.persons().get(0).name());
        System.out.println("15) findByNameIgnoreCase -> " + o15);
    }

    // 문제 메서드들은 기본적으로 UnsupportedOperationException 을 던집니다.
    // 구현 후 직접 호출해서 결과를 출력/검증해 보세요.

    // 예시) 구현 후 테스트
    // var m = topNProductsByCategory(data.products(), 3);
    // m.entrySet().stream().limit(3).forEach(System.out::println);
}
