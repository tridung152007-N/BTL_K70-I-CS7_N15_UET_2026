/**
 * Interface hỗ trợ tìm kiếm và lọc
 */
package com.auction.observer;

public interface ISearchable {
    boolean match(String keyword); // Kiểm tra từ khóa khớp với tên/mô tả
}