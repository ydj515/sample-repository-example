package com.example.springaisample.tool.shopping;

import java.util.List;

import com.example.springaisample.model.ProductItem;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class ShoppingTools {

    private final ShoppingCatalogService shoppingCatalogService = new ShoppingCatalogService();

    // ToolContext에 직접 입력한 사용자 ID를 이용
    @Tool(description = "특정 ID 사용자의 구매 목록을 조회. ")
    List<ProductItem> getOrderedByCustomer(ToolContext toolContext) {
        String userId = (String) toolContext.getContext().get("userId");
        return shoppingCatalogService.getOrderedByCustomer(userId);
    }

    @Tool(description = "고객이 주로 구매한 카테고리와 구매가격을 기반으로 제품을 검색해줘. ")
    List<ProductItem> getContents(@ToolParam(description = "카테고리", required = true) String category) {
        return shoppingCatalogService.getContents(category);
    }
}
