/**
 * Interface hỗ trợ tìm kiếm và lọc
 */
public interface ISearchable {
    boolean match(String keyword); // Kiểm tra từ khóa khớp với tên/mô tả
}