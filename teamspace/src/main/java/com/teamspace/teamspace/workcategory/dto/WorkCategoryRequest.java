package com.teamspace.teamspace.workcategory.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter; import lombok.Setter;
@Getter @Setter
public class WorkCategoryRequest {
 @NotBlank @Size(max=100) private String name;
 @NotBlank @Pattern(regexp="^#[0-9a-fA-F]{6}$") private String color;
 @NotBlank @Size(max=50) private String icon;
 private Long version;
}
