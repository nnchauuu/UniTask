package com.teamspace.teamspace.workcategory.dto;
import java.util.List; import jakarta.validation.constraints.NotEmpty; import lombok.Getter; import lombok.Setter;
@Getter @Setter public class WorkCategoryOrderRequest { @NotEmpty private List<Long> categoryIds; }
