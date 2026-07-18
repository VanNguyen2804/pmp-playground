# Export toàn bộ câu hỏi ra CSV

## API

```http
GET /api/questions/export/csv
```

Endpoint tải toàn bộ câu hỏi đang lưu trong database, không phụ thuộc bộ lọc hoặc trang hiện tại trên frontend.

Response:

- `Content-Type: text/csv;charset=UTF-8`
- `Content-Disposition: attachment; filename="pmp_questions_YYYYMMDD_HHMMSS.csv"`
- File có UTF-8 BOM để Excel hiển thị tiếng Việt đúng.

## Một dòng cho mỗi câu hỏi

| Column | Ý nghĩa |
|---|---|
| `id` | ID nội bộ |
| `externalId` | ID từ PMA hoặc nguồn import |
| `examName` | Tên đề thi |
| `questionType` | `MCQ`, `MRQ` hoặc `MATCHING` |
| `questionText` | Nội dung câu hỏi |
| `categoryCodes` | Mã category, phân cách bằng ` | ` |
| `categoryNames` | Tên category, phân cách bằng ` | ` |
| `currentAnswerKeys` | Đáp án đúng hiện đang lưu, ví dụ `B` hoặc `B | D` |
| `currentAnswerText` | Nội dung đáp án đúng; với Matching là các cặp `left -> right` |
| `allOptions` | Toàn bộ lựa chọn của MCQ/MRQ |
| `source` | Nguồn câu hỏi |
| `difficulty` | Độ khó |
| `updatedAt` | Thời điểm cập nhật gần nhất |

Frontend có nút **Xuất toàn bộ CSV** trên trang **Ngân hàng câu hỏi PMP**. Trạng thái download dùng Angular Signal `exporting` và được cập nhật ngay khi backend trả response.
