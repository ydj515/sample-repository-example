package com.example.springaisample.tool.shopping;

import java.util.List;

import com.example.springaisample.model.ProductItem;
import org.springframework.stereotype.Service;

@Service
public class ShoppingCatalogService {

    // 데이터베이스 정보 조회
    public List<ProductItem> getOrderedByCustomer(String id) {
        if ("id01".equals(id)) {
            return List.of(
                    new ProductItem(100, "청반바지", 50000, "바지", "빨강"),
                    new ProductItem(101, "기모바지", 20000, "바지", "주황"),
                    new ProductItem(102, "청바지", 40000, "바지", "노랑"),
                    new ProductItem(103, "흰바지", 30000, "바지", "빨강"),
                    new ProductItem(104, "여름바지", 40000, "바지", "주홍")
            );
        }

        if ("id02".equals(id)) {
            return List.of(
                    new ProductItem(200, "후드티", 10000, "상의", "검정"),
                    new ProductItem(201, "반팔티", 20000, "상의", "회색"),
                    new ProductItem(202, "긴팔티", 10000, "상의", "흰색"),
                    new ProductItem(203, "기모티", 15000, "상의", "Gray"),
                    new ProductItem(204, "셔츠", 10000, "상의", "Black")
            );
        }

        return List.of();
    }

    // 데이터베이스 정보 조회
    public List<ProductItem> getContents(String category) {
        if ("바지".equals(category)) {
            return List.of(
                    new ProductItem(105, "청반바지1", 100000, "바지", "빨강"),
                    new ProductItem(106, "기모바지2", 200000, "바지", "주황"),
                    new ProductItem(107, "청바지3", 40000, "바지", "노랑"),
                    new ProductItem(108, "흰바지4", 30000, "바지", "빨강"),
                    new ProductItem(109, "여름바지5", 40000, "바지", "주홍")
            );
        }

        if ("상의".equals(category)) {
            return List.of(
                    new ProductItem(205, "후드티1", 100000, "상의", "검정"),
                    new ProductItem(206, "반팔티2", 20000, "상의", "회색"),
                    new ProductItem(207, "긴팔티3", 10000, "상의", "흰색"),
                    new ProductItem(208, "기모티4", 150000, "상의", "Gray"),
                    new ProductItem(209, "셔츠5", 10000, "상의", "Black")
            );
        }

        return List.of();
    }
}
