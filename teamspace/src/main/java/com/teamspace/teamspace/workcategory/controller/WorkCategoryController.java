package com.teamspace.teamspace.workcategory.controller;
import java.util.List; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import com.teamspace.teamspace.common.ApiResponse; import com.teamspace.teamspace.workcategory.dto.*; import com.teamspace.teamspace.workcategory.service.WorkCategoryService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class WorkCategoryController { private final WorkCategoryService service;
 @GetMapping("/projects/{projectId}/work-categories") public ApiResponse<List<WorkCategoryResponse>> list(@PathVariable Long projectId,Authentication a){return ApiResponse.success("Lay linh vuc thanh cong",service.list(projectId,a));}
 @PostMapping("/projects/{projectId}/work-categories") public ApiResponse<WorkCategoryResponse> create(@PathVariable Long projectId,@Valid @RequestBody WorkCategoryRequest r,Authentication a){return ApiResponse.success("Tao linh vuc thanh cong",service.create(projectId,r,a));}
 @PutMapping("/work-categories/{id}") public ApiResponse<WorkCategoryResponse> update(@PathVariable Long id,@Valid @RequestBody WorkCategoryRequest r,Authentication a){return ApiResponse.success("Cap nhat linh vuc thanh cong",service.update(id,r,a));}
 @PutMapping("/projects/{projectId}/work-categories/order") public ApiResponse<List<WorkCategoryResponse>> order(@PathVariable Long projectId,@Valid @RequestBody WorkCategoryOrderRequest r,Authentication a){return ApiResponse.success("Sap xep linh vuc thanh cong",service.order(projectId,r,a));}
 @PostMapping("/work-categories/{id}/deactivate") public ApiResponse<WorkCategoryResponse> deactivate(@PathVariable Long id,Authentication a){return ApiResponse.success("Ngung su dung linh vuc thanh cong",service.active(id,false,a));}
 @PostMapping("/work-categories/{id}/activate") public ApiResponse<WorkCategoryResponse> activate(@PathVariable Long id,Authentication a){return ApiResponse.success("Kich hoat linh vuc thanh cong",service.active(id,true,a));}
}
