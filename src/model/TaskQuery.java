package model;

/**
 * Encapsulates search, filter, sort, and pagination criteria.
 * Built using the Builder pattern; all fields are optional.
 */
public final class TaskQuery {
    private final Boolean completed;
    private final Priority priority;
    private final String dueDate;
    private final String searchKeyword;
    private final Integer parentId;
    private final String sortBy;
    private final String sortOrder; // "ASC" or "DESC"
    private final int page;         // 1-indexed
    private final int pageSize;

    private TaskQuery(Builder builder) {
        this.completed = builder.completed;
        this.priority = builder.priority;
        this.dueDate = builder.dueDate;
        this.searchKeyword = builder.searchKeyword;
        this.parentId = builder.parentId;
        this.sortBy = builder.sortBy != null ? builder.sortBy : "id";
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder.toUpperCase() : "ASC";
        this.page = builder.page > 0 ? builder.page : 1;
        this.pageSize = builder.pageSize > 0 ? builder.pageSize : 10;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Boolean getCompleted() {
        return completed;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public Integer getParentId() {
        return parentId;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public static final class Builder {
        private Boolean completed;
        private Priority priority;
        private String dueDate;
        private String searchKeyword;
        private Integer parentId;
        private String sortBy;
        private String sortOrder;
        private int page = 1;
        private int pageSize = 10;

        public Builder completed(Boolean completed) {
            this.completed = completed;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder dueDate(String dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder searchKeyword(String searchKeyword) {
            this.searchKeyword = searchKeyword;
            return this;
        }

        public Builder parentId(Integer parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder sortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public TaskQuery build() {
            return new TaskQuery(this);
        }
    }
}
