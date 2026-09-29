package com.campusfind.service;

import com.campusfind.dto.CategoryDto;
import com.campusfind.dto.CategoryRequest;
import com.campusfind.entity.Category;
import com.campusfind.exception.BadRequestException;
import com.campusfind.exception.ConflictException;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.CategoryRepository;
import com.campusfind.repository.FoundItemRepository;
import com.campusfind.repository.LostReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final LostReportRepository lostReportRepository;
    private final FoundItemRepository foundItemRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           LostReportRepository lostReportRepository,
                           FoundItemRepository foundItemRepository) {
        this.categoryRepository = categoryRepository;
        this.lostReportRepository = lostReportRepository;
        this.foundItemRepository = foundItemRepository;
    }

    public List<CategoryDto> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    public CategoryDto getCategoryById(Long id) {
        Category category = findEntityById(id);
        return CategoryDto.fromEntity(category);
    }

    public Category findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category with name '" + name + "' already exists.");
        }

        Category category = new Category(
                name,
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getActive() != null ? request.getActive() : true
        );

        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest request) {
        Category category = findEntityById(id);
        String newName = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(newName, id)) {
            throw new ConflictException("Category with name '" + newName + "' already exists.");
        }

        category.setName(newName);
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription().trim());
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category updated = categoryRepository.save(category);
        return CategoryDto.fromEntity(updated);
    }

    @Transactional
    public CategoryDto updateCategoryStatus(Long id, boolean active) {
        Category category = findEntityById(id);
        category.setActive(active);
        Category updated = categoryRepository.save(category);
        return CategoryDto.fromEntity(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findEntityById(id);

        if (lostReportRepository.existsByCategoryId(id) || foundItemRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete category '" + category.getName() +
                    "' because existing lost reports or found items reference it. Please deactivate the category instead.");
        }

        categoryRepository.delete(category);
    }
}
